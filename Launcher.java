
public class Launcher {

    //Parameters
    private static int NumberOfClient = 10;
    private static int port = 4444;
    private static String file = "crap.pdf";
    private static int maxProbFail =5;
    private static int windowSize = 4; //windowsize for gobackn
    
    public static void main(String[] args) {

        server s = new server();
        String[] serverArgs = {Integer.toString(port), Integer.toString(NumberOfClient), file,Integer.toString(windowSize)};
        s.setArgs(serverArgs);

        s.start();



        //Random r = new Random();


        client[] clients = new client[NumberOfClient];
        for(int i = 0; i < NumberOfClient; i++) {
            clients[i] = new client();
            String[] tmp = {Integer.toString(port),Integer.toString(NumberOfClient), Integer.toString(i), Integer.toString(maxProbFail),Integer.toString(windowSize)};
            clients[i].setArgs(tmp);
            clients[i].start();
        }
    }

    public static String byteToPrefixByte(long n) {
        char[] prefixes = {'K', 'M', 'G', 'T'};
        int pref = -1;

        String formated = "(~";

        while (n > 1000 && pref <= 3) {
            n = n / 1000;
    
            pref++;
        }

        if (pref == -1) {
            return "";
        }
        formated += Long.toString(n) + prefixes[pref] + "B" + ")";
        return formated;
    }
}
