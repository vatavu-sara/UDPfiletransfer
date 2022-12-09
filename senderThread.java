import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class senderThread extends Thread {
    private DatagramSocket server;

    private InetAddress addr; //the addres to send to
    private int port; // the port to send to

    private int size; //size of data
    private byte[] data;

    private int packetsSent = 0;
    private long bytesSend = 0;

    private int pnb;
    private int cnb;


    senderThread(DatagramSocket server, InetAddress addr, int port, int size, byte[] data, int packetNumber, int clientNumber) {
        super();

        this.server = server;

        this.addr = addr;
        this.port = port;

        this.data = data;
        this.size = size;

        pnb = packetNumber;
        cnb = clientNumber;
    }

    @Override
    public void run() {
        super.run();

        DatagramPacket packet = null;
        DatagramPacket packetLength= null;
        DatagramPacket status=null;

        int received = 0;

        System.out.println("Sending to client " + cnb + " at " + addr + ":" + port);

        try {
            
        //copy past of sara's code :p with a bit of editing
        while (received == 0) { //send until the client confirm he has received the packet
            byte[] buffer2 = new byte[10];

            // sending the packet length
            buffer2 = Integer.toString(size).getBytes();
            packetLength = new DatagramPacket(buffer2, buffer2.length,addr,port);
            server.send(packetLength);
            
            // sending the packet itself
            packet = new DatagramPacket(data, size,addr,port);
            server.send(packet);

            // receive status of packet
            status = new DatagramPacket(buffer2, buffer2.length);
            server.receive(status);
            received = Integer.parseInt(new String(status.getData(), 0, status.getLength()));

            if(received == 0) { //just there to send message if it fail after (could use do while but not the priority at the moment)
                System.out.println("Failed : Resending packet " + pnb + " to client " + cnb);
            }
            packetsSent++;
            bytesSend += size;
        }
        } catch (Exception e) {
            e.printStackTrace();
        }


        System.out.println("Packet no. " + pnb + " sent to client" +cnb+ "! Length:" + size);
        
    }    

    //used for stats at the end
    public int getNBPacketSent() {
        return packetsSent;
    }

    public long getNBByteSent() {
        return bytesSend;
    }
}
