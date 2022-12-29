import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class receiverThread extends Thread{
    private DatagramSocket server;

    private int port;
    private InetAddress addr;
    private long ack;


    receiverThread(DatagramSocket server, int port, InetAddress addr) {
        super();
        this.server = server;
        this.port = port;
        this.addr = addr;

        ack = 0;
    }

    @Override
    public void run() {
        try {
            byte[] buff = new byte[10];
            DatagramPacket p = new DatagramPacket(buff, buff.length, addr, port);
            server.receive(p);

            //if no timeout...
            ack = Long.parseLong(new String(p.getData(), 0, p.getLength()));

        } catch (SocketTimeoutException e) {
            //timed out = either client did not receive or ack from client got lost 
            /*
             * For the sake of simulation we will pretend that if the packet was lost no ack is send by the client
             */
            ack = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    public long getAck() {return ack;}
}
