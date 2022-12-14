import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class senderThreadRDT extends Thread {
    private DatagramSocket server;

    private InetAddress addr; //the addres to send to
    private int port; // the port to send to

    private int size; //size of data
    private byte[] data;

    private int packetsSent = 0;
    private long bytesSend = 0;

    private long seqNumber = 0;
    private long ackNumber = 0;

    private int pnb;
    private int cnb;


    senderThreadRDT(DatagramSocket server, InetAddress addr, int port, int size, byte[] data, int packetNumber,long seqNumber, int clientNumber) {
    
        this.server = server;

        this.addr = addr;
        this.port = port;

        this.data = data;
        this.size = size;

        this.seqNumber=seqNumber;

        pnb = packetNumber+1; //added +1 to start from 1 not from 0
        cnb = clientNumber; //same
    }

    @Override
    public void run() {
        super.run();

        DatagramPacket packet = null;
        DatagramPacket packetLength= null;
        DatagramPacket status=new DatagramPacket(data, 10);

        //otherwise the threads would keep mixing themselves
        status.setAddress(addr);
        status.setPort(port);


        //System.out.println("From "+Thread.currentThread()+"Sending to client " + cnb + " at " + addr + ":" + port);

        try {
            
        //copy past of sara's code :p with a bit of editing
        while (true) { //send until the client confirm he has received the packet

            byte[] buffer2 = new byte[10];

            // sending the packet length
            buffer2 = Integer.toString(size).getBytes();
            packetLength = new DatagramPacket(buffer2, buffer2.length,status.getAddress(),status.getPort());
            server.send(packetLength);
            
            // sending the packet itself
            packet = new DatagramPacket(data, size,status.getAddress(),status.getPort());
            server.send(packet);

            buffer2=new byte[100];
            // receive status of packet
            status = new DatagramPacket(buffer2, buffer2.length);
            server.receive(status);
            ackNumber = Long.parseLong(new String(status.getData(), 0, status.getLength()));

            packetsSent++;
            bytesSend += size;

            if(ackNumber == seqNumber+size) { //just there to send message if it fail after (could use do while but not the priority at the moment)
                break;
                
            }
               //so here we should add a timeout so i just put a sleep of 0.1s
                //sleep(100);
            //System.out.println("From "+Thread.currentThread()+": Failed : Resending packet "+ pnb+ " to client " +cnb);
        }
        } catch (Exception e) {
            e.printStackTrace();
        }


        
    }    

    //used for stats at the end
    public int getNBPacketSent() {
        return packetsSent;
    }

    public long getNBByteSent() {
        return bytesSend;
    }

    public long getAckNumber() {
        return ackNumber;
    }
    
}