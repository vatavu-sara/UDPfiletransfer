import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class server extends DatagramSocket implements Runnable{
    static int MAX_PKT_SZ = 65507; //in bytes
    static int MAX_DATA_SZ = MAX_PKT_SZ - 10; //10 being the size reserverd for the packet number

    private int noc; //number of client, server wait for all of them before starting the transfer
    private int winsz; //window size
    private ArrayList<byte[]> winPackets;

    private String filePath;

    private int packetsNeeded;

    private FileInputStream FIS;

    private int steps;

    private senderThread[][] senders;
    private receiverThread[][] receivers;
    private verificationThread[] verifiers;

    //Clients info
    private InetAddress[] addrs;
    private int[] ports;
    private long[][] seqs;
    private long baseWinSeq;

    //stats
    private long[] bytesSent;
    private int[] packetsSent;

    //misc
    int size; //size of the newly read bite of the file, if zero it mean we arrived at the end of the file.


    server(String filePath, int winsz, int noc) throws SocketException {
        super(8000);
        this.setSoTimeout(100);

        this.filePath = filePath;
        this.winsz = winsz;
        this.noc = noc;

        steps = 0;

        senders = new senderThread[winsz][noc];
        receivers = new receiverThread[winsz][noc];
        verifiers = new verificationThread[noc];

        addrs = new InetAddress[noc]; //addresses for all the clients
        ports = new int[noc]; //ports for all the clients

        seqs = new long[winsz][noc];
        for (int i = 0; i < winsz; i++) {
            for (int j = 0; j < noc; j++) {
                seqs[i][j] = 1;
            }
        }

        baseWinSeq = 1;

        bytesSent = new long[noc];
        packetsSent = new int[noc];
    }

    private void connect() throws IOException {
        /*
         * Wait for a response of noc clients and then send a acknoledgement message to all of them when everyone is ready
         */
        for (int i = 0; i < noc; i++) {
            byte[] buff = new byte[2]; //wil contain the number of the client that has send the packet

            DatagramPacket p = new DatagramPacket(buff, buff.length);  
            this.receive(p);

            int position = Integer.parseInt(new String(p.getData(), 0, p.getLength()));
            addrs[position] = p.getAddress();
            ports[position] = p.getPort();
            bytesSent[position] = 0;
            packetsSent[position] = 0;
            System.out.println("Client " + position + ": " + addrs[position] + ":" + ports[position]);
        }

        //when the loops end...
        for (int i = 0; i < noc; i++) {
            byte[] buff = new byte[100];
            buff = filePath.getBytes();
            DatagramPacket p = new DatagramPacket(buff, buff.length, addrs[i], ports[i]);
            this.send(p);
        }

        //when this ends : we have all info about everyone and we can start the upload

        System.out.println("All ("+ noc + ") clients are connected;");
        
    }

    private byte[] readFile() throws IOException {
        /*
         * Reads on packet (at most MAX_DATA_SZ bytes) in FIS
         */
        byte[] data = new byte[MAX_DATA_SZ];

        size = 0;
        while (size < MAX_DATA_SZ && FIS.available() != 0) {
            data[size] = (byte) FIS.read();
            size++;
        }
        if (size != MAX_DATA_SZ) { size--;}
        
        return data;

        
    }

    private void updateWindow(int n) {
        /*delete the n first element of window and replace them by new elements */
        for (int i = 0; i < n; i++) {
            byte[] data = new byte[MAX_DATA_SZ];
            try {
                data = readFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if(size != 0) {
                //if the are more packets to read...
                winPackets.remove(0);
                winPackets.add(data);
            } else {
                //if there are not enoght packet for the desired windowsize, reduce the window size
                winsz -= i;
            }
        }
    }

    @Override
    public void run() {
        try {
            connect(); //conect to everybody

            FIS = new FileInputStream(new File(filePath)); //to read data from the file we need to send

            //compute the number of packet needed
            packetsNeeded = (FIS.available() / MAX_PKT_SZ) - 1 ;
            

            //perform some sanity checks
            winsz = (winsz> packetsNeeded) ? packetsNeeded : winsz; //in case the window size is to big :(

            
            for (int i = 0; i < noc; i++) { // communicates with all client and send them the number of packet needed for the file transfer
                byte[] buffer = new byte[10];
                buffer = Integer.toString(packetsNeeded).getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, addrs[i], ports[i]);
                this.send(packet);
            }
            
            //do the sending
            winPackets = new ArrayList<byte[]>();
            //fills the winpacket with the first winsz packets at first;
            for (int i = 0; i < winsz; i++) {
                winPackets.add(readFile());
            }

            do {
                /*
                 * steps :
                 *     V - Send packet win to winsz
                 *     V - wait for evey thread to have finish sending and receiving/timing out
                 *     v - Check who hasn't received what and get the lowest packet that hasn't been send
                 *     v - add ordinal number of packet that were succesfully sent to steps (can be 0...) and advance the window size from that amount
                 *     v - resend the whole window size
                 */

                for(int curr = 0; curr<winsz; curr++) {
                    for(int i = 0; i<noc; i++) {
                        //for every packet in the window size and for every client ->
                        //create a senderthread that will send winsz packets to all clients
                        senders[curr][i] = new senderThread(this, ports[i], addrs[i], winPackets.get(curr), seqs[curr][i], i, bytesSent[i], packetsSent[i], curr);
                        senders[curr][i].start();
                    }
                }

                for(int curr = 0; curr<winsz; curr++) {
                    for(int i = 0; i<noc; i++) {
                        //now we wait for everyone to finish sending
                        senders[curr][i].join(); //note : lots of info remains stored on the sender threads
                        // we extract the bytes and pcket sends
                        bytesSent[i] += senders[curr][i].getByteS();
                        packetsSent[i] += senders[curr][i].getPackS();

                        //now we can start the listeners
                        receivers[curr][i] = new receiverThread(this, ports[i], addrs[i]);
                        receivers[curr][i].start();
                    }
                }

                for(int curr = 0; curr<winsz; curr++) {
                    for(int i = 0; i<noc; i++) {
                        //now we wait for everybody to finish listening, they either crash (= timeout) or actually read a value for the ack (timeout will result in ack = 0)
                        receivers[curr][i].join();
                        seqs[curr][i] = receivers[curr][i].getAck();
                    }
                }

                //now we shall compare if the seqs are correct
                /*
                 * Steps :
                 * - build array of received acks for every client
                 * - launch a verification for every said array
                 * - get the smalest number from the verification (if any)
                 * - update the base seq
                 * - advance the window accordingly
                 */
                for(int i = 0; i<noc; i++) {
                    long[] tmp = new long[winsz];
                    long[] tmpLen = new long[winsz];
                    for(int curr = 0; curr<winsz; curr++) {
                       tmp[curr] = receivers[curr][i].getAck();
                       tmpLen[curr] = senders[curr][i].getByteS();
                    }
                    verifiers[i] = new verificationThread(tmp, winsz, baseWinSeq, tmpLen);
                    verifiers[i].start();
                }

                //now we wait for every verification to be performed
                int[] failurePoints = new int[noc];
                for (int j = 0; j < noc; j++) {
                    verifiers[j].join();
                    failurePoints[j] = verifiers[j].getFailurePoint();
                }

                int minFailurePoint = minOfArray(failurePoints);
                if (minFailurePoint > winsz) {
                    //great no packet were lost !
                    System.out.println("No packet lost, proceeding to next window...");
                    steps += winsz;
                } else {
                    System.out.println("Need to restart from element " + minFailurePoint + " in window ("+ winsz +")...");
                    steps += minFailurePoint;
                }

                updateWindow(minFailurePoint);
                long[] lastgoodSeqs = new long[noc];
                for (int i = 0; i < lastgoodSeqs.length; i++) {
                    lastgoodSeqs[i]  = verifiers[i].getLastGoodSeq();
                }
                long lastGoodSeq = minOfArray(lastgoodSeqs);
                baseWinSeq = lastGoodSeq; //next iteration seq number will start at the last ack that was received from all client
                


            } while (steps < packetsNeeded);

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public long minOfArray(long[] arr) {
        long min = arr[0];
        
        for(int i=0; i<arr.length; i++ ) {
            if(arr[i]<min) {
                min = arr[i];
            }
        }
        return min;
    }

}
