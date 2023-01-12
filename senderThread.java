import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
@SuppressWarnings("unused")

//copy paste of original thread but with modifications
public class senderThread extends Thread {
    private DatagramSocket server;
    DatagramPacket status;


    private InetAddress addr; //the addres to send to
    private int port; // the port to send to

    private int size; //size of data
    private byte[] data;

    private int packetsSent = 0;
    private long bytesSend = 0;

    private long seqNumber = 0;


    private int pnb;
    private int cnb;


    senderThread(DatagramSocket server, InetAddress addr, int port, int size, byte[] data, int packetNumber,long seqNumber, int clientNumber) {
    
        this.server = server;

        this.addr = addr;
        this.port = port;

        this.data = data;
        this.size = size;

        this.seqNumber=seqNumber;

        pnb = packetNumber; //added +1 to start from 1 not from 0
        cnb = clientNumber; //same
    }

    @Override
    public void run() {

        DatagramPacket packet = null;

        //System.out.println("From "+Thread.currentThread()+"Sending to client " + cnb + " at " + addr + ":" + port);

        try {

        
            byte[] buffer2 =  Long.toString(seqNumber).getBytes();
            byte [] sizeBytes= Integer.toString(size).getBytes();
            byte[] packetData= new byte[65507];

            //System.out.println("size:"+size);  
                      
            //adding the seq no to the packet data
            for(int i=0;i<buffer2.length;i++)
                packetData[i]= buffer2[i];
            
            //if its length <10 the rest will be _
            for(int i=buffer2.length;i<10;i++)
                packetData[i]='_';

            //adding the size
            for(int i=0;i<sizeBytes.length;i++)
                packetData[i+10]= sizeBytes[i];
            for(int i=sizeBytes.length+10;i<15;i++)
                packetData[i]='_';

            //copying all the file data to the packet
            System.arraycopy(data, 0, packetData, 15, data.length);
            
            // System.out.println("SENDING Packet no. " + pnb + " to client" +cnb+ "! Length:" + size);

            // sending the packet itself
            packet = new DatagramPacket(packetData, packetData.length,addr,port);
            server.send(packet);

            packetsSent++;
            bytesSend += size;
                
        } catch (Exception e) {
            e.printStackTrace();
        }

        
    }    

    

    public long getSeqNumber() {
        return seqNumber;
    }

    public DatagramSocket getServer() {
        return server;
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
}
