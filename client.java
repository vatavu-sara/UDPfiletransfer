import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

public class client extends DatagramSocket implements Runnable{
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
    private long byteRecived;

    //formating
    private String offset;



    client(int nb, int serverPort, int probFail) throws SocketException, UnknownHostException {
        super();
        serverName =  InetAddress.getLocalHost();

        this.nb = nb;
        this.serverPort = serverPort;
        this.probFail = probFail;

        ack = 1;

        rng = new Random();

        packetsReceived = 0;
        byteRecived = 0;

        //creates the dir for the client
        dirName = defDirName+nb;
        System.out.println(dirName);
        new File(dirName).mkdir();

        offset = "-".repeat(nb+1) + "(" + nb + ")";
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
            
            do {
                //first receive the seq
                buff = new byte[10];
                DatagramPacket p = new DatagramPacket(buff, buff.length, serverName, serverPort);
                this.receive(p);

                //parse the received seq;
                long tmpAck = Long.parseLong(new String(p.getData(), 0, p.getLength())); 

                //receives the packet length
                buff = new byte[10];
                p = new DatagramPacket(buff, buff.length, serverName, serverPort);
                this.receive(p);

                //parse it
                int len = Integer.parseInt(new String(p.getData(), 0, p.getLength()));


                //recives the packet itself
                byte[] data = new byte[len];
                p = new DatagramPacket(data, len, serverName, serverPort);
                this.receive(p);

                //evalutate if the stuff needs to fail
                int chance = rng.nextInt(100);

                //buff and packet for the ack number
                buff = new byte[10];

                //depending on the RNG we send the previous ack or the new one
                ack = (chance > probFail) ? ack : (ack+tmpAck);


                //if the packet was "correctly" received we write it;
                if (chance <= probFail) {

                    System.out.println(offset + "Succesfully received packet n°" + packetsReceived);

                    packetsReceived++;
                    FOS.write(data, 0, len);
				    FOS.flush();
                } else {
                    System.out.println(offset + "Failled to recieve packet n°" + packetsReceived + " awaiting resend...");
                }

                //sends the ack
                buff = Long.toString(ack).getBytes();
                p = new DatagramPacket(buff, buff.length, serverName, serverPort);
                this.send(p);


            } while(packetsNeeded > packetsReceived);

            this.close();
            System.out.println(offset + "All ("+ packetsNeeded + ") received, closing...");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
}
