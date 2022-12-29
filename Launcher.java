import java.net.SocketException;
import java.net.UnknownHostException;

public class Launcher {
    static server s;
    static client[] clients;

    static int noc = 2;
    public static void main(String[] args) {
        try {
            s = new server("Partie1.txt", 3, noc);

            new Thread(s).start();
            clients = new client[noc];

            for (int i = 0; i < noc; i++) {
                clients[i] = new client(i, 8000, 0, 2);
                new Thread(clients[i]).start();;
            }



        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
}