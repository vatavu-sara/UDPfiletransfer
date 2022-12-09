import java.io.IOException;
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
    private int bytesSend = 0;
    private int received;


    senderThread(DatagramSocket server, InetAddress addr, int port, int size, byte[] data) {
        super();

        this.server = server;

        this.addr = addr;
        this.port = port;

        this.data = data;
        this.size = size;
    }

    @Override
    public void run() {
        super.run();

        DatagramPacket packet = null;
        DatagramPacket packetLength= null;
        DatagramPacket status=null;
        DatagramPacket bytes=null;

        System.out.println("sending...");

        try {
            
        //copy past of sara's code :p
        byte buffer2[] = new byte[10];
               
        //send packet length
        buffer2 = Integer.toString(size).getBytes();
        packetLength = new DatagramPacket(buffer2, buffer2.length,addr,port);
        server.send(packetLength);
        

        // sending the packet itself
        packet = new DatagramPacket(data, size,addr, port);
        server.send(packet);
        // receive status of packet (1 received 0 failed)
        status = new DatagramPacket(buffer2, buffer2.length);
        server.receive(status);
        received = Integer.parseInt(new String(status.getData(), 0, status.getLength()));

        packetsSent++;
        bytesSend += size;
        while (received == 0) { 
            buffer2 = new byte[10];

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
            packetsSent++;
            bytesSend += size;
        }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }


        //System.out.println("Packet no. " + packets + " sent to client" +i+ "!Length:" + size);
        

        /*if (packets == packetsNeeded) {
            // sending total bytes sent
            buffer = Long.toString(bytesSend[i]).getBytes();
            bytes = new DatagramPacket(buffer, buffer.length, addresses[i], cl_ports[i]);
            server.send(bytes);

            // sending total packets sent
            buffer = Integer.toString(packetsSent[i]).getBytes();
            bytes = new DatagramPacket(buffer, buffer.length, addresses[i], cl_ports[i]);
            server.send(bytes);
            System.out.println("Client "+i +"  fully received the file!");
            // server.close();
            // System.out.println("Connection closed");
            
        }*/
    }    
}
