import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class senderThread extends Thread{

    private byte[] toSend;

    private int port;
    private InetAddress addr;

    private DatagramSocket server;

    private long seq;

    private int nbc;

    private int pktNb;

    private long byteS;
    private int packS;

    senderThread(server s, int port, InetAddress addr, byte[] packet, long seq, int idClient, long byteS, int packS, int packetNb) {
        super();

        server = s; 
        this.port = port;
        this.addr = addr;

        this.seq = seq;

        nbc = idClient;

        this.packS = packS;
        this.byteS = byteS;

        pktNb = packetNb;



    }

    @Override
    public void run() {
        try {
            System.out.println("Sending packet n°" + pktNb + "to client n°" + nbc + "...");
            /*
                * Steps : 
                * -Build the packet with seq + data;
                * - Send it
                * 
                */
            byte[] buff = new byte[65507];

            //creates the packet to be send
            byte[] seqByte = Long.toString(seq).getBytes();

            System.arraycopy(seqByte, 0, buff, 0, seqByte.length);
            System.arraycopy(toSend, 0, buff, seqByte.length, toSend.length);

            DatagramPacket p = new DatagramPacket(buff, buff.length, addr, port);
            server.send(p);

            byteS += buff.length;
            packS++;


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public long getSeq() {return seq;}
    public int getIdClient() {return nbc;}
    public long getByteS() {return byteS;}
    public int getPackS() {return packS;}

}
