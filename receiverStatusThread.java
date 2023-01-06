import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class receiverStatusThread extends Thread{

    private DatagramSocket server;
    private long ackNumber;
    private int pnb;
    private int cnb;


    public long getAckNumber() {
        return ackNumber;
    }

    public int getPnb() {
        return pnb;
    }

    public int getCnb() {
        return cnb;
    }

    receiverStatusThread(DatagramSocket server, int packetNumber, int clientNumber) {
    
        this.server = server;

        pnb = packetNumber; //added +1 to start from 1 not from 0
        cnb = clientNumber; //same
    }

    @Override
    public void run(){
         // receive status of packet
         byte[] buffer2 = new byte[21];
         DatagramPacket status = new DatagramPacket(buffer2, buffer2.length);
         try {
            server.receive(status);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        byte ackBytes[]= new byte[10];
        byte pnbBytes[]= new byte[10];


        //ack number from client(to see if it received or not)
        for(int i=0;i<10&&status.getData()[i]!='_';i++)
        ackBytes[i]=status.getData()[i];

        ackNumber = Long.parseLong(new String(ackBytes).trim());

        //packet number from client(to see what we receive)
        for(int i=10;i<status.getLength()&&status.getData()[i]!='_';i++)
        pnbBytes[i-10]=status.getData()[i];


        pnb=Integer.parseInt(new String(pnbBytes).trim());
        cnb=(int) status.getData()[20];

        System.out.println("[ReceiverThread] client:"+cnb+" packet " +pnb+" ackNb:"+ackNumber);
    
    }
}
