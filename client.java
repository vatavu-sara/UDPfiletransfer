import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

public class client extends DatagramSocket implements Runnable{
    static int MAX_PKT_SZ = 65507; //in bytes
    //client related info
    private int nb; //id of the client
    private int probFail; //probability of failure of a client

    private Random rng; 

    private long ack; //acknumber

    private static String defDirName = "clients/client";
    private String dirName;

    private FileOutputStream FOS;

    //server related info
    private InetAddress serverName;
    private int serverPort; //number of client
    private int packetsNeeded;
    private int packetsReceived;
    private int winsz;
    private long minseq;
    private long maxseq;

    //formating
    private String offset;

    //packets
    ArrayList<DatagramPacket> packetList;



    client(int nb, int serverPort, int probFail, int serverwsz) throws SocketException, UnknownHostException {
        super();
        serverName =  InetAddress.getLocalHost();

        this.nb = nb;
        this.serverPort = serverPort;
        this.probFail = probFail;

        ack = 1;

        rng = new Random();

        winsz = serverwsz;

        packetsReceived = 0;

        //creates the dir for the client
        dirName = defDirName+nb;
        System.out.println(dirName);
        new File(dirName).mkdir();

        offset = "-".repeat(nb+1) + "(" + nb + ")";

        packetList = new ArrayList<DatagramPacket>();

        this.setSoTimeout(1000);
    }

    private void connect() throws IOException {
        //send a hello to the server with the client's id
        byte[] buff = new byte[10];
		buff = Integer.toString(nb).getBytes();
		DatagramPacket packet = new DatagramPacket(buff, buff.length, serverName, serverPort);
		this.send(packet);


        buff = new byte[100];//resets buffer

		// receive ack that all clients are connected(as the filename)
		packet = new DatagramPacket(buff, buff.length);
		this.receive(packet);

        String filename = new String(packet.getData(), 0, packet.getLength());
        
        FOS = new FileOutputStream(dirName+"/"+filename);

        System.out.println(offset + "Everybody is connected to server ! ");
        
    }

    private long getSeq(DatagramPacket p) {
        /*reads the first ten bytes in the packet*/
        byte[] buff = new byte[10];
        int count = 0;

        for (int i = 0; i < 10; i++) {
            if (p.getData()[i] != 0) {
                System.out.println(count);
                count ++;
                buff[i] = p.getData()[i];
            }
        }
        
        long seq = Long.valueOf(new String(buff, 0, count));

        return seq;
    }

    private long getAck(DatagramPacket p) {
        return getSeq(p)+ p.getLength();
    }

    private void flushPackets() throws IOException {
        /*
         * steps :
         * sort packets by seq
         * perform some check to see if the order is good
         * send ack for each packet
         * write to file in order
         * reset buffer
         */

        DatagramPacket[] sortedPackets = new DatagramPacket[packetList.size()];
        //convert arraylist to list manually

        for (int i = 0; i < sortedPackets.length; i++) {
            sortedPackets[i] = packetList.get(i);
        }

        System.out.println("Succesfully converted");

        //then do the stuff

        for (int i = 1; i < sortedPackets.length; i++) {
            DatagramPacket key = sortedPackets[i];
            int j = i-1;

            while(j>=0 && getSeq(sortedPackets[i]) > getSeq(key)) {
                sortedPackets[j+1] = sortedPackets[j];
                j--;
            }

            sortedPackets[j+1] = key;
        }

        //now that we sorted, they might be a place where we have a packet missing : we must check for the continuousity of the packets to make sure we don't write a packet in the place of a missing one
        int failurePoints = 0;

        for (int i = 0; i < sortedPackets.length; i++) {//compare each seq with the pevious one (check is prev ack = new seq
            if(i==0) {
                if (minseq != getSeq(sortedPackets[i])) {
                    failurePoints = i;
                }
            } else {
                if (getAck(sortedPackets[i-1]) != getSeq(sortedPackets[i])) {
                    failurePoints = i;
                }
            }
        }

        //the we delete everything from the failure point
        for (int i = 0; i < sortedPackets.length; i++) {
            if (i >= failurePoints) {
                sortedPackets[i] = null;
            }
        }

        //after the check, we can send to ack for the remaining packet (aka eveything that isn't null in sortedPackets)
        for (int i = 0; i < sortedPackets.length; i++) {
            if (sortedPackets[i] != null) {
                byte[] ack = Long.toString(getAck(sortedPackets[i])).getBytes();

                DatagramPacket p = new DatagramPacket(ack, ack.length, serverName, serverPort);
                this.send(p);
                packetsReceived ++; //if the packet was received, checked and then its ack was send then we can mark it as received)

                //also write to file
                FOS.write(sortedPackets[i].getData(), 10, sortedPackets[i].getLength() - 10);
            }
        }

        //in the end we reset the buffer;
        packetList = new ArrayList<DatagramPacket>();
        FOS.flush();
    }

    @Override
    public void run() {
        try {
            connect();

            byte[] buff = new byte[10];
            //receives the number of packets needed for the tranfer
            
            DatagramPacket packet = new DatagramPacket(buff, buff.length);
            this.receive(packet);

            //parse the number of packets needed;
            packetsNeeded = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));

            System.out.println(offset + "The file will come in " + packetsNeeded + " packets.");

            //set the range for the acceptable seqs
            minseq = ack;
            maxseq = minseq + winsz * MAX_PKT_SZ;

            int flushCounter = 0;
            
            do {
                flushCounter++;

                if (flushCounter == winsz-1) {
                    //do the stuf with sorting, writing and updating the min and max
                    flushPackets();
                }

                buff = new byte[MAX_PKT_SZ];

                DatagramPacket p = new DatagramPacket(buff, MAX_PKT_SZ, serverName, serverPort);
                try {
                    this.receive(p);
                } catch (SocketTimeoutException e) {//if we timeout, try again (avoids deadlocks)
                    continue;
                }
                

                //simulate failure
                if (probFail < rng.nextInt(100)) {
                    long s = getSeq(p);
                    //get the seq of the newly received packet, then compare if it fits the range we defined
                    if (s < minseq || s > maxseq) {
                        //discard packet
                        System.out.println(offset + "Discarding packet, wrong seq...");
                        continue;
                    }
                    //we will count packetsRecevied once we sorted and checked them

                    packetList.add(p);

                }

                

            } while(packetsNeeded > packetsReceived);

            this.close();
            System.out.println(offset + "All ("+ packetsNeeded + ") received, closing...");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
}
