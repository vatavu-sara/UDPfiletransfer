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
         bytesSend[position] = 0;
         packetsSent[position] = 0;
         System.out.println("Client " + position + ": " + addresses[position] + ":" + cl_ports[position]);

      }

      //send to each client (j) all the addresses 
      for(int i=0;i<noc;i++)
         for(int j=0;j<noc;j++)
         {  
            buffer=new byte[6];
            buffer=Integer.toString(cl_ports[i]).getBytes();
            packet= new DatagramPacket(buffer, buffer.length,addresses[j],cl_ports[j]);
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
                     System.out.println(
                           "Packet " + packets + " will be sent to client " + i + " Seq: " + seqNumber + "Len:" + size);
                     threads[i] = new senderThreadRDT(server, addresses[i], cl_ports[i], size, data, packets, seqNumber,
                           i);
                     //if this join loop isnt here even this crashes :)
                     for(int j=0;j<i;j++)
                     threads[j].join();
                     threads[i].start();
                  }
                  for (int i = 0; i < noc; i++) {
                     // once every thread is launched wait for them to finish before proceeding
                     threads[i].join();
                     // System.out.println("finished waiting for client " + (i+1));
                     packetsSent[i] += threads[i].getNBPacketSent();
                     bytesSend[i] += threads[i].getNBByteSent();
                     seqNumber = threads[i].getAckNumber();
                  }

                  
                  System.out.println("All clients have received the packet " + (packets++) +
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
               ArrayList<Long> seqNumberExpected = new ArrayList<Long>(windowSize);

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


               while (packets < packetsNeeded) {
                  senderThreadGBN[] threads = new senderThreadGBN[noc];

                  // create a new thread for each client to send packet 0 in the list
                  for (int i = 0; i < noc; i++)
                  {
                     System.out.println("Creating thread no " + packets + " for client " + i);
                     threads[i]= new senderThreadGBN(server, addresses[i], cl_ports[i], sizes.get(0),
                           packetsList.get(0), packets, seqNumber, i);
                     //if this join loop isnt here even this crashes :)
                     for(int j=0;j<i;j++)
                     threads[j].join();
                     threads[i].start();
                  }
                  seqNumber = seqNumber + sizes.get(0);
                  seqNumberExpected.add(seqNumber);
                  
                  int allReceived;
                  do {
                     allReceived = 1;
                     for(int i=0; i<noc ;i++)
                     threads[i].join();

                     for (int i = 0; i < noc; i++) {
                        System.out.println("Waiting for packet " + packets + " from client " + i);
                        packetsSent[i] += threads[i].getNBPacketSent();
                        bytesSend[i] += threads[i].getNBByteSent();
                        System.out.println(threads[i].getStatusnb());
                        if (threads[i].getStatusnb() != 1) {
                           try {
                              sleep(100);// the timeout basically
                           } catch (Exception e) {
                              // TODO: handle exception
                           }
                           System.out.println("Packet " + packets + " not received by client " + i + " Resending...");
                              //restarting the thread basically
                              senderThreadGBN tmp = new senderThreadGBN(threads[i].getServer(),
                                    threads[i].getAddr(), threads[i].getPort(),
                                    threads[i].getSize(), threads[i].getData(), packets,
                                    threads[i].getSeqNumber(), i);
                              threads[i] = new senderThreadGBN(tmp.getServer(), tmp.getAddr(), tmp.getPort(),
                                    tmp.getSize(), tmp.getData(), packets, tmp.getSeqNumber(), i);
                              //threads[i].setAckNumber(tmp.getAckNumber());
                              threads[i].start();
                           
                           allReceived = 0;
                           break;}
                        }
                        
                     
                     if (allReceived == 1) {
                        System.out.println("All good for packet " + packets++);
                        //send to all clients we good
                        for (int i = 0; i < noc; i++) {
                           buffer = new byte[2];
                           buffer = Integer.toString(1).getBytes();
                           packet = new DatagramPacket(buffer, buffer.length, addresses[i], cl_ports[i]);
                           server.send(packet);
                        }

                        packetsList.remove(0);
                        sizes.remove(0);
                        seqNumberExpected.remove(0);

                        if (packets <= (packetsNeeded - windowSize)) {
                           byte data[] = new byte[65507];

                           // Generate the data used for the next packet
                           int size = 0;
                           while (size < 65507 && fis.available() != 0) {
                              data[size] = (byte) fis.read();
                              size++;
                           }
                           if (size != 65507)
                              size--;

         

                           packetsList.add(data);
                           sizes.add(size);
                           seqNumber = seqNumber + sizes.get(sizes.size() - 1);
                           seqNumberExpected.add(seqNumber);
                           
                     
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
   int totalPacketSent = 0;for(
   int i:packetsSent)
   {
      totalPacketSent += i;
   }

   long totalByteSent = 0;for(
   long i:bytesSend)
   {
      totalByteSent += i;
   }

   finish=Instant.now();
   FileOutputStream stream = new FileOutputStream("stats");
   Duration elapsed = Duration.between(start,
         finish);stream.write("\nStats :\n\tGlobal :\n".getBytes());stream.write(("\t\t Time elapsed: m:"+elapsed.toMinutes()+" s:"+elapsed.toSeconds()%60+" ms:"+elapsed.toMillis()%1000+"\n").getBytes());stream.write(("\t\t- Total packet sent to all ("+noc+") clients : "+totalPacketSent+"\n").getBytes());stream.write(("\t\t- Total byte sent to all ("+noc+") clients : "+totalByteSent+" "+Launcher.byteToPrefixByte(totalByteSent)+"\n").getBytes());

   stream.write("\n\tBy client :\n".getBytes());for(
   int i = 0;i<noc;i++)
   {
      float estimatedProbFail = (1 - ((float) packetsNeeded / packetsSent[i])) * 100;
      stream.write(("\t\t- Client no " + i + ":\n").getBytes());
      stream.write(("\t\t\t- Packets sent : " + packetsSent[i] + "\n").getBytes());
      stream.write(("\t\t\t- Bytes sent : " + bytesSend[i] + " " + Launcher.byteToPrefixByte(bytesSend[i]) + "\n")
            .getBytes());
      stream.write(("\t\t\t- Estimated Probability of Faillure : " + estimatedProbFail + "\n").getBytes());
   }

   stream.close();System.out.println("You can find statistical report in ./stats");

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
