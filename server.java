// server.java

import java.net.*;
import java.io.*;
import java.time.Instant;
import java.util.ArrayList;

import java.time.Duration;

@SuppressWarnings("unused")

public class server extends Thread {
   private String[] args;
   static int MAX_PKT_SZ = 65507; // in bytes
   static int MAX_DATA_SZ = MAX_PKT_SZ - 15; // 10 being the size reserverd for the seq nr and 5 for the size
   int size; // size of the newly read bite of the file, if zero it mean we arrived at the
             // end of the file.

   public void main(String[] args) throws IOException {
      if (args.length != 5) {
         System.out.println("Arguments not valid!(Need 2)");
         System.exit(0);
      }

      Instant start, finish;
      start = Instant.now();
      int rcv_port = Integer.parseInt(args[0]);
      DatagramSocket server = new DatagramSocket(rcv_port);
      System.out.println("Server started!");
      DatagramPacket packet = null;
      int noc = Integer.parseInt(args[1]); // number of clients
      String fileName = args[2];
      String algorithm = args[3];
      int windowSize = Integer.parseInt(args[4]);
      InetAddress addresses[] = new InetAddress[noc];
      int cl_ports[] = new int[noc];

      int packets = 0; // no of packets sent for each file
      long bytesSend[] = new long[noc];
      long bytesReceived[] = new long[noc];

      long seqNumber = 1;
      int packetsSent[] = new int[noc];
      byte[] buffer = new byte[10];

      // connecting to the clients
      for (int i = 0; i < noc; i++) {
         packet = new DatagramPacket(buffer, buffer.length);
         server.receive(packet);
         int position = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
         addresses[position] = packet.getAddress();
         cl_ports[position] = packet.getPort();
         bytesReceived[position] = 0;
         bytesSend[position] = 0;
         packetsSent[position] = 0;
         System.out.println("Client " + position + ": " + addresses[position] + ":" + cl_ports[position]);

      }

      // send to each client (j) all the addresses
      for (int i = 0; i < noc; i++)
         for (int j = 0; j < noc; j++) {
            buffer = new byte[6];
            buffer = Integer.toString(cl_ports[i]).getBytes();
            packet = new DatagramPacket(buffer, buffer.length, addresses[j], cl_ports[j]);
            server.send(packet);
         }

      // sending acknowledge we are connected to all clients(as the filename)
      for (int i = 0; i < noc; i++) {
         buffer = new byte[100];
         buffer = fileName.getBytes();
         packet = new DatagramPacket(buffer, buffer.length, addresses[i], cl_ports[i]);
         server.send(packet);
      }

      System.out.println("All client connected\nSending file: " + fileName + " to all (" + noc + ") clients.");

      // making file input stream(to read from it)
      FileInputStream fis = new FileInputStream(new File(fileName));

      // max 65507 but 10 are seqnr/acknr

      int packetsNeeded = fis.available() / MAX_DATA_SZ + 1; // nr of packets needed
      System.out.println("File (" + fis.available() + " bytes) Needs " + packetsNeeded + " packets to transmit "
            + fileName + "\n\n");
      if (windowSize > packetsNeeded)
            windowSize = packetsNeeded;

      for (int i = 0; i < noc; i++) {
         // send nr of packets needed and windowsize
         buffer = new byte[10];
         buffer = Integer.toString(packetsNeeded).getBytes();
         packet = new DatagramPacket(buffer, buffer.length, addresses[i], cl_ports[i]);
         server.send(packet);
      }

            /*
             * steps :
             * V - Send packet win to winsz
             * V - wait for evey thread to have finish sending and receiving/timing out
             * v - Check who hasn't received what and get the lowest packet that hasn't been
             * send
             * v - add ordinal number of packet that were succesfully sent to steps (can be
             * 0...) and advance the window size from that amount
             * v - resend the whole window size
             */

            ArrayList<Long> ackNumberExpected = new ArrayList<Long>(windowSize);
            int restartFrom = 0;
            int restartFromArray[]= new int[noc];

           
            // System.out.println("Go-back-N method chosen! Window size:" + windowSize);

            // creating the N packets' data list
            ArrayList<byte[]> packetsList = new ArrayList<byte[]>(windowSize);
            ArrayList<Integer> sizes = new ArrayList<Integer>(windowSize);
            long ackNumbers[][] = new long[noc][windowSize];
            long tmpseq = 1;

            for (int i = 0; i < windowSize; i++) {
               // sending data in packets of 65507-15
               byte data[] = new byte[MAX_DATA_SZ];

               // Generate the data used for the packet
               size = 0;
               data = readFile(fis);

               System.out.println("Packet " + i + "added to arraylist size:" + size);
               packetsList.add(data);
               sizes.add(size+15);
               tmpseq = tmpseq + sizes.get(i); //we add the 15 bytes used also to the seqnr
               ackNumberExpected.add(tmpseq);
            }
               for(int i=0;i<noc;i++)
                  restartFromArray[i]=0;

            while (packets < packetsNeeded) {
               try {
                  senderThreadGBN[][] threads = new senderThreadGBN[noc][windowSize];
                  receiverStatusThread[][] rcvthreads = new receiverStatusThread[noc][windowSize];

                  // create a new thread for each client to send the n packets in the list +start
                  // sending it
                  System.out.println(
                        "Attempting to send packets " + packets + "-" + (packets + windowSize - 1) + " to all clients");
                  
                     //for each client we start sending from the first packet lost to the limit of the window
                     for (int i = 0; i < noc; i++) {
                        //but first we need to send them how many packets got received by every client
                        buffer = new byte[5];
                        buffer = Integer.toString(restartFrom).getBytes();
                        packet = new DatagramPacket(buffer, buffer.length, addresses[i], cl_ports[i]);
                        server.send(packet);
                        
                        //we try to transmit only the packets that are missing for the client up to the windowSize
                        System.out.println("Restart from = "+restartFromArray[i]);
                        for (int j = restartFromArray[i]; j < windowSize; j++) {
                        System.out.println("(Re)Starting thread no " + (packets + j) + " for client " + i);

                        //calculate the seqnr as 1+the size of all packets sent before
                        long sq=seqNumber;
                        for(int k=0;k<j;k++)
                           sq+=sizes.get(k);
                        threads[i][j] = new senderThreadGBN(server, addresses[i], cl_ports[i], sizes.get(j),
                              packetsList.get(j), (packets + j), sq, i);
                        // this join loop is meant to make the packets sequential in a client (otherwise there is the posibility we get an upper packet 
                        //before a lower and that is too hard to fix, we LOVE UDP
                        // for (int k = restartFromArray[i]; k < j; k++)
                        // threads[i][k].join();
                        threads[i][j].start();
                        rcvthreads[i][j] = new receiverStatusThread(server, packets + j, i);
                        //and we make it sequential too 
                        // for (int k = 0; k < j; k++)
                        // rcvthreads[i][k].join();
                        rcvthreads[i][j].start();
                        }
                     
                  }
               
               

                  System.out.println("Waiting for packets");

                     for (int i = 0; i < noc; i++)
                     //for each client we are checking only the packets missing prior to the (re)sending 
                        for (int j = restartFromArray[i]; j < windowSize; j++) {

                        //once the sending has finished
                        threads[i][j].join();

                        //regardless of the receiving the packet j well, the bytes are still sent
                        packetsSent[i] += threads[i][j].getNBPacketSent();
                        bytesSend[i] += threads[i][j].getNBByteSent();

                        //once the confirmation/timeout has arrived
                        rcvthreads[i][j].join();
                       
                        System.out.println("Client "+ rcvthreads[i][j].getCnb() + " packet " + (rcvthreads[i][j].getPnb()-packets) 
                        +" in windowsize has ack "+ rcvthreads[i][j]
                        .getAckNumber());

                        //we save the acknumber received 
                        ackNumbers[rcvthreads[i][j].getCnb()][rcvthreads[i][j].getPnb()-packets] = rcvthreads[i][j]
                              .getAckNumber();
                           
                     }
                 
                  //the happiest scenario is when all clients received every packet so we can just move the WS 
                  restartFrom = windowSize;
                  
                  // now we got all ack no's so we know where every client lost the first packet
                     for (int i = 0; i < noc; i++){
                        //we remember the position where the client started receiving packets this round
                        int idx=restartFromArray[i];
                        //and we reset the breaking point
                        restartFromArray[i]=windowSize;
                        System.out.println("Client " + i +":");
                        //for all the packets sent we need to check whether it was received or not
                        //using the ack' number
                        for (int j = idx; j < windowSize; j++){
                        System.out.println("Packet "+ j + " Real ack:" + ackNumbers[i][j] + " Expected ack:" + ackNumberExpected.get(j));
                        if (ackNumbers[i][j] != ackNumberExpected.get(j)) {
                           System.out.println(
                                 "First packet got lost at position " + j + " in the window size (client" + i);
                           restartFrom = restartFrom < j ? restartFrom : j ;
                           restartFromArray[i]=j;
                           break; 
                        }
                     }
                  }

                  //in the end we substract the nr of packets received by everyone for each client's reset point
                  //because the window will be moved by this amount
                  for (int i = 0; i < noc; i++)
                     restartFromArray[i]-=restartFrom;

                  if (restartFrom ==windowSize)
                     System.out.println("All packets in the windowsize got received");
                  else
                  if (restartFrom != 0)
                     System.out.println("All good for the first " + restartFrom + " packets");
                  else 
                     System.out.println("A client lost the first packet");

               
                  tmpseq = ackNumberExpected.get(ackNumberExpected.size()-1);
                  // we can get rid of the packets that went good
                  for (int i = 0; i < restartFrom; i++) {
                     System.out.println("Packet " + packets++ + " received by everyone and removed from the list!");
                     // the seqnr of the server increses
                     seqNumber += sizes.get(0);
                     // we remove the first packet from the list
                     packetsList.remove(0);
                     // as well as the size
                     sizes.remove(0);
                     // as well as the expected ack
                     ackNumberExpected.remove(0);
                  }

                  for (int i = 0; i < restartFrom; i++) {
                     // and if there are still packets in the file we read the next one
                     if (packets <= (packetsNeeded - windowSize)) {
                        byte data[] = new byte[MAX_DATA_SZ];

                        // Generate the data used for the next packet
                        size = 0;
                        data = readFile(fis);

                        packetsList.add(data);
                        sizes.add(size+15);
                        System.out.println("Packet " + (packets+i+(windowSize-restartFrom)) + " added to arraylist size:" + size);
                        tmpseq += size+15;
                        ackNumberExpected.add(tmpseq);
                     }
                     // otherwise the windowsize decreases
                     else
                        windowSize--;
                  }
               } catch (Exception e) {
                  System.out.println("We catch an exception but what??");
               }
            }
     
      // receive by all clients byresReceived
      for (int i = 0; i < noc; i++) {
         // send ok that all finished
         byte[] signal = new byte[2];
         signal = "OK".getBytes();
         packet = new DatagramPacket(signal, signal.length, addresses[i], cl_ports[i]);
         server.send(packet);
         System.out.println("Do we even get here");
         // receive nr of bytes
         buffer = new byte[100];
         packet = new DatagramPacket(buffer, buffer.length);
         server.receive(packet);
         bytesReceived[i] = Long.parseLong(new String(packet.getData(), 0, packet.getLength()));
         System.out.println(bytesReceived[i]);

      }

      server.close();// we don't need it anymore
      System.out.println("Sending file completed closing socket.");

      // stats
      int totalPacketSent = 0;

      for (int i : packetsSent) {
         totalPacketSent += i;
      }

      long totalByteSent = 0;
      for (long i : bytesSend) {
         totalByteSent += i;
      }

      long totalByteReceived = 0;
      for (long i : bytesReceived) {
         totalByteReceived += i;
      }

      finish = Instant.now();
      FileOutputStream stream = new FileOutputStream("stats");
      Duration elapsed = Duration.between(start,
            finish);
      stream.write("\nStats :\n\tGlobal :\n".getBytes());
      stream.write(("\t\t Time elapsed: m:" + elapsed.toMinutes() + " s:" + elapsed.toSeconds() % 60 + " ms:"
            + elapsed.toMillis() % 1000 + "\n").getBytes());
      stream.write(
            ("\t\t- Total packet received to all (" + noc + ") clients : " + (packetsNeeded * noc) + "\n").getBytes());
      stream.write(("\t\t- Total packet sent to all (" + noc + ") clients : " + totalPacketSent + "\n").getBytes());
      stream.write(("\t\t- Total packet retransmitted to all (" + noc + ") clients : "
            + (totalPacketSent - packetsNeeded * noc) + "\n").getBytes());
      stream.write(("\t\t- Total byte received by all (" + noc + ") clients : " + totalByteReceived + " " +

            byteToPrefixByte(totalByteReceived) + "\n").getBytes());
      stream.write(("\t\t- Total byte sent to all (" + noc + ") clients : " + totalByteSent + " "
            + byteToPrefixByte(totalByteSent) + "\n").getBytes());
      stream.write(("\t\t- Total byte retransmitted to all (" + noc + ") clients : "
            + (totalByteSent - totalByteReceived) + " "
            + byteToPrefixByte(totalByteSent - totalByteReceived) + "\n").getBytes());

      stream.write("\n\tBy client :\n".getBytes());
      for (int i = 0; i < noc; i++) {
         float estimatedProbFail = (1 - ((float) packetsNeeded / packetsSent[i])) * 100;

         stream.write(("\t\t- Client no " + i + ":\n").getBytes());
         stream.write(("\t\t\t- Packets received : " + packetsNeeded + "\n").getBytes());
         stream.write(("\t\t\t- Packets sent : " + packetsSent[i] + "\n").getBytes());
         stream.write(("\t\t\t- Packets retransmiited : " + (packetsSent[i] - packetsNeeded) + "\n").getBytes());
         stream.write(("\t\t\t- Bytes received : " + bytesReceived[i] + " " + byteToPrefixByte(bytesReceived[i]) + "\n")
               .getBytes());
         stream.write(("\t\t\t- Bytes sent : " + bytesSend[i] + " " + byteToPrefixByte(bytesSend[i]) + "\n")
               .getBytes());
         stream.write(("\t\t\t- Bytes retransmitted : " + (bytesSend[i] - bytesReceived[i]) + " "
               + byteToPrefixByte(bytesSend[i] - bytesReceived[i]) + "\n")
               .getBytes());
         stream.write(("\t\t\t- Estimated Probability of Failure : " + estimatedProbFail + "\n").getBytes());
      }

      stream.close();
      System.out.println("You can find statistical report in ./stats");

   }

   // to run the server
   public void setArgs(String[] args) {
      this.args = args;
   }

   @Override
   public void run() {
      // TODO Auto-generated method stub
      super.run();
      try {
         main(args);
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

   }

   private byte[] readFile(FileInputStream fis) throws IOException {
      /*
       * Reads on packet (at most MAX_DATA_SZ bytes) in FIS
       */
      byte[] data = new byte[MAX_DATA_SZ];

      size = 0;
      while (size < MAX_DATA_SZ && fis.available() != 0) {
         data[size] = (byte) fis.read();
         size++;
      }
      if (size != MAX_DATA_SZ) {
         size--;
      }

      return data;

   }

   public static String byteToPrefixByte(long n) {
      char[] prefixes = { 'K', 'M', 'G', 'T' };
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
