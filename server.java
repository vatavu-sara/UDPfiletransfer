// server.java

import java.net.*;
import java.io.*;
import java.time.Instant;
import java.util.ArrayList;
import java.time.Duration;

public class server extends Thread {
   private String[] args;

   public static void main(String[] args) throws IOException {
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
      int packetsSent[] = new int[noc];
      byte[] buffer = new byte[10];

      // connecting to the clients
      for (int i = 0; i < noc; i++) {
         packet = new DatagramPacket(buffer, buffer.length);
         server.receive(packet);
         int position = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
         addresses[position] = packet.getAddress();
         cl_ports[position] = packet.getPort();
         bytesSend[position] = 0;
         packetsSent[position] = 0;
         System.out.println("Client " + position + ": " + addresses[position] + ":" + cl_ports[position]);

      }

      // sending acknowledge we are connected to all clients
      for (int i = 0; i < noc; i++) {
         buffer = new byte[10];
         buffer = fileName.getBytes();
         packet = new DatagramPacket(buffer, buffer.length, addresses[i], cl_ports[i]);
         server.send(packet);
      }

      System.out.println("All client connected\nSending file: " + fileName + " to all (" + noc + ") clients.");

      // making file input stream(to read from it)
      FileInputStream fis = new FileInputStream(new File(fileName));

      // max 65507 ?
      int packetsNeeded = fis.available() / 65507 + 1; // nr of packets needed
      System.out.println("Needs " + packetsNeeded + " packets to transmit " + fileName + "\n\n");

      for (int i = 0; i < noc; i++) {
         // send nr of packets needed
         buffer = new byte[10];
         buffer = Integer.toString(packetsNeeded).getBytes();
         packet = new DatagramPacket(buffer, buffer.length, addresses[i], cl_ports[i]);
         server.send(packet);
      }
      switch (algorithm) {
         case "RDT":
         System.out.println("Reliable Data Transfer 3.0 method chosen!");
            while (packets < packetsNeeded) {
               try {
                  // sending data in packets of 65507
                  byte data[] = new byte[65507];

                  // Generate the data used for the packet
                  int size = 0;
                  while (size < 65507 && fis.available() != 0) {
                     data[size] = (byte) fis.read();
                     size++;
                  }
                  if (size != 65507)
                     size--;

                  // array of senderThread (thread that manage the process of sending the file to
                  // a client and resending if necessary)
                  senderThreadRDT[] threads = new senderThreadRDT[noc];
                  for (int i = 0; i < noc; i++) {
                     // create a new thread and start it
                     threads[i] = new senderThreadRDT(server, addresses[i], cl_ports[i], size, data, packets, i);
                     threads[i].start();
                  }
                  for (int i = 0; i < noc; i++) {
                     // once every thread is launched wait for them to finish before proceeding
                     threads[i].join();
                     // System.out.println("finished waiting for client " + (i+1));
                     packetsSent[i] += threads[i].getNBPacketSent();
                     bytesSend[i] += threads[i].getNBByteSent();
                  }

                  packets++;
                  System.out.println("All clients have received the packet "+ (packets) +
                   "\n");

               } catch (SocketTimeoutException s) {
                  System.out.println("Socket timed out!");
                  break;
               } catch (IOException e) {
                  e.printStackTrace();
                  break;
               } catch (InterruptedException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
               }
            }
            break;
         case "goBackN":
            try {
               if (windowSize > packetsNeeded)
                  windowSize = packetsNeeded;
               System.out.println("Go-back-N method chosen! Window size:" + windowSize);

               // creating the N packets' data list
               ArrayList<byte[]> packetsList = new ArrayList<byte[]>(windowSize);
               ArrayList<Integer> sizes = new ArrayList<Integer>(windowSize);
               for (int i = 0; i < windowSize; i++) {
                  // sending data in packets of 65507
                  byte data[] = new byte[65507];

                  // Generate the data used for the packet
                  int size = 0;
                  while (size < 65507 && fis.available() != 0) {
                     data[size] = (byte) fis.read();
                     size++;
                  }
                  if (size != 65507)
                     size--;

                  System.out.println("Packet " + i + "added to arraylist");
                  packetsList.add(data);
                  sizes.add(size);
               }

               senderThreadGBN[][] threads = new senderThreadGBN[noc][windowSize];
               for (int j = 0; j < windowSize; j++)
                  for (int i = 0; i < noc; i++)
                  // create a new thread for the first N packets for each client and start it
                  {
                     System.out.println("Creating thread no " + j + " for client " + i);
                     threads[i][j] = new senderThreadGBN(server, addresses[i], cl_ports[i], sizes.get(j),
                           packetsList.get(j), j, i);

                     for(int k=0;k<=j-1;k++)  
                        threads[i][k].join();
                     threads[i][j].start();
                  }

               while (packets < packetsNeeded) {
                  int allReceived;

                  do {
                     allReceived = 1;
                     for (int i = 0; i < noc; i++) {
                        threads[i][0].join();
                        System.out.println("Waiting for packet " + packets + " from client " + i);
                        packetsSent[i] += threads[i][0].getNBPacketSent();
                        bytesSend[i] += threads[i][0].getNBByteSent();
                        if (threads[i][0].getReceived() == 0) {
                           System.out.println("Packet " + packets + " not received by client " + i + " Resending...");
                           for (int j = 0; j < windowSize; j++) {
                                 threads[i][j].join();
                                 System.out.println("Resend packet"+(packets+j));
                                 senderThreadGBN tmp = threads[i][j];
                                 threads[i][j] = new senderThreadGBN(tmp.getServer(), tmp.getAddr(), tmp.getPort(),
                                       tmp.getSize(), tmp.getData(), packets + j, i);
                                 for(int k=0;k<=j-1;k++)  
                                    threads[i][k].join();
                                 threads[i][j].start();
                                 System.out.println("Break after start thread"+(packets+j));
                              
                           }
                           allReceived = 0;
                           break;
                        }
                     }
                     if (allReceived == 1) {

                        System.out.println("All good for packet " + packets++);
                        packetsList.remove(0);

                        if (packets <= packetsNeeded - windowSize)
                        // sending data in packets of 65507
                        {
                           byte data[] = new byte[65507];

                           // Generate the data used for the packet
                           int size = 0;
                           while (size < 65507 && fis.available() != 0) {
                              data[size] = (byte) fis.read();
                              size++;
                           }
                           if (size != 65507)
                              size--;

                           packetsList.add(data);
                           sizes.add(size);

                           for (int k = 0; k < noc; k++) {
                              {
                                 for (int j = 0; j < windowSize - 1; j++)
                                    threads[k][j] = threads[k][j + 1];
                              }
                              threads[k][windowSize - 1] = new senderThreadGBN(server, addresses[k], cl_ports[k],
                                   size,
                                    data, packets + windowSize - 1, k);
                              for(int j=0;j<windowSize-1;j++)  
                                    threads[k][j].join();
                              threads[k][windowSize - 1].start();

                           }
                        }
                     }

                  } while (allReceived == 0);

               }
            } catch (Exception e) {
               // TODO: handle exception
            }
            break;
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

      finish = Instant.now();
      FileOutputStream stream = new FileOutputStream("stats");
      Duration elapsed = Duration.between(start, finish);
      stream.write("\nStats :\n\tGlobal :\n".getBytes());
      stream.write(("\t\t Time elapsed: m:" + elapsed.toMinutes() + " s:" + elapsed.toSeconds() % 60 + " ms:"
            + elapsed.toMillis() % 1000 + "\n").getBytes());
      stream.write(("\t\t- Total packet sent to all (" + noc + ") clients : " + totalPacketSent + "\n").getBytes());
      stream.write(("\t\t- Total byte sent to all (" + noc + ") clients : " + totalByteSent + " "
            + Launcher.byteToPrefixByte(totalByteSent) + "\n").getBytes());

      stream.write("\n\tBy client :\n".getBytes());
      for (int i = 0; i < noc; i++) {
         float estimatedProbFail = (1 - ((float) packetsNeeded / packetsSent[i])) * 100;
         stream.write(("\t\t- Client no " + i + ":\n").getBytes());
         stream.write(("\t\t\t- Packets sent : " + packetsSent[i] + "\n").getBytes());
         stream.write(("\t\t\t- Bytes sent : " + bytesSend[i] + " " + Launcher.byteToPrefixByte(bytesSend[i]) + "\n")
               .getBytes());
         stream.write(("\t\t\t- Estimated Probability of Faillure : " + estimatedProbFail + "\n").getBytes());
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
}
