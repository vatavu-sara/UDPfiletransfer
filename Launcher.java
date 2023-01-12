

public class Launcher {

    //Parameters
    private static int NumberOfClient = 2;
    private static int port = 4444;
    private static String file = "Docker.mp4";
    private static int maxProbFail =5;
    private static int windowSize =3; //windowsize for gobackn
    private static int repeat = 1;
    
    public static void main(String[] args) {

        double ttime = 0;
        for (int r = 0; r < repeat; r++) {
            System.out.println((r+1) + "/" + repeat);
            server s = new server();
            String[] serverArgs = {Integer.toString(port), Integer.toString(NumberOfClient), file,Integer.toString(windowSize)};
            s.setArgs(serverArgs);

            s.start();


            client[] clients = new client[NumberOfClient];
            for(int i = 0; i < NumberOfClient; i++) {
                clients[i] = new client();
                String[] tmp = {Integer.toString(port),Integer.toString(NumberOfClient), Integer.toString(i), Integer.toString(maxProbFail),Integer.toString(windowSize)};
                clients[i].setArgs(tmp);
                clients[i].start();
            }
            System.out.println("Transmiting...");
            try {
                s.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            ttime += s.getTime();
        }
        System.out.println("TIME : " + ttime);
    }
}
