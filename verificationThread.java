public class verificationThread extends Thread {

    private boolean succes;
    private int failurePoint;
    private long lastgoodseq;

    private long[] expectedSeqs;

    private long[] rawSeqs;
    private int winsz;
    private long baseSeq;
    private long[] lenghtsSend;

    verificationThread(long[] rawSeqs, int windowSize, long baseSeq, long[] lenghtsSend) {
        super();
        succes = false;
        failurePoint=windowSize + 1;

        expectedSeqs = new long[windowSize];

        this.rawSeqs = rawSeqs;
        winsz = windowSize;
        this.baseSeq = baseSeq;
        this.lenghtsSend = lenghtsSend;
    }

    @Override
    public void run() {
        /*
         * steps
         * calculate the expected seqs
         * compare whit the rawSeqs
         */

        //build expected seqs
        expectedSeqs[0] = baseSeq + lenghtsSend[0];
        for (int i = 1; i < winsz; i++) {
            expectedSeqs[i] = expectedSeqs[i-1] + lenghtsSend[i];
        }

        //now compare with the raw seqs (wich can (and probably will) be unordered)
        /*
         * Steps:
         * - Look at the n-th expected seq and see if any of the raw seqs corresponds
         * - if not then the n-th packet was not received -> set succes to false and failure point to n and break
         * - otherwise continue
         * - if all seqs are found -> set succes to true;
         */

        for (int n = 0; n < winsz; n++) {
            boolean test = false;
            for (int i = 0; i < winsz; i++) {
                test = (expectedSeqs[n] == rawSeqs[i]) ? true : false;
            }
            //if test remains unaltered, then there was an issue at packet N
            if(!test) {
                succes = false;
                failurePoint = n;
                lastgoodseq = expectedSeqs[n-1];
                return;
            }
        }
        //if the function is still running (every seq is good)
        lastgoodseq = expectedSeqs[expectedSeqs.length-1];
        return;
    }
    
    public boolean getSucces() {return succes;}
    public int getFailurePoint() {
        /*
         * Basicaly from where in the window do we need to restart for this specific client.
         */
        return failurePoint;
    } 
    public long getLastGoodSeq() {
        return lastgoodseq;
    }
}
