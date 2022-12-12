import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


//copy paste of original thread but with modifications
public class senderThreadGBN extends Thread {
    private DatagramSocket server;

    private InetAddress addr; //the addres to send to
    private int port; // the port to send to

    private int size; //size of data
    private byte[] data;

    private int packetsSent = 0;
    private long bytesSend = 0;

    private int pnb;
    private int cnb;

    private int received=0;

    senderThreadGBN(DatagramSocket server, InetAddress addr, int port, int size, byte[] data, int packetNumber, int clientNumber) {
    
        this.server = server;

        this.addr = addr;
        this.port = port;

        this.data = data;
        this.size = size;

        pnb = packetNumber; //added +1 to start from 1 not from 0
        cnb = clientNumber; //same
    }

    @Override
    public void run() {
        super.run();

        DatagramPacket packet = null;
        DatagramPacket packetLength= null;
        DatagramPacket status=new DatagramPacket(data, 10);

        //System.out.println("From "+Thread.currentThread()+"Sending to client " + cnb + " at " + addr + ":" + port);

        try {
            
        //copy past of sara's code :p with a bit of editing
        //send only once!

            byte[] buffer2 = new byte[10];

            // sending the packet length
            System.out.println("Sending the size:"+size);
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

            if(received==0)
            System.out.println("Received should be 0 for pck "+pnb +" in c"+cnb+"\n");
            else
            System.out.println("Received should be 1 for pck "+pnb +" in c"+cnb+"\n");

            packetsSent++;
            bytesSend += size;

                
        } catch (Exception e) {
            e.printStackTrace();
        }


        //System.out.println("From "+Thread.currentThread()+"Packet no. " + pnb + " sent to client" +cnb+ "! Length:" + size);
        
    }    

    public InetAddress getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    public int getSize() {
        return size;
    }

    public byte[] getData() {
        return data;
    }

    //used for stats at the end
    public int getNBPacketSent() {
        return packetsSent;
    }

    public long getNBByteSent() {
        return bytesSend;
    }

    //to get received or not
    public int getReceived() {
        return received;
    }
}
