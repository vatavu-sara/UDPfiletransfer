// server.java

import java.net.*;
import java.io.*;

public class server {
   public static void main(String[] args) throws IOException {
      if(args.length!=3) {
			System.out.println("Arguments not valid!(Need 2)");
			System.exit(0);
		}
      int rcv_port = Integer.parseInt(args[0]);
      DatagramSocket server = new DatagramSocket(rcv_port);
      System.out.println("Server started!");
      DatagramPacket packet = null;
      int noc = Integer.parseInt(args[1]); // number of clients
      String fileName = args[2];
      InetAddress addresses[] = new InetAddress[11];
      int cl_ports[] = new int[10];


      int packets = 0; // no of packets sent for each file
      long bytesSend[] = new long[10];
      int packetsSent[] = new int[10];
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
         System.out.println("Client " + i + ": "+addresses[i]+":"+cl_ports[i]);

      }

      // sending acknowledge we are connected to all clients
      for (int i = 0; i < noc; i++) {
         buffer = new byte[10];
         buffer = Integer.toString(1).getBytes();
         packet = new DatagramPacket(buffer, buffer.length, addresses[i], cl_ports[i]);
         server.send(packet);
      }

      System.out.println("All client connected\nSending file: " + fileName + " to all ("+noc+") clients.");

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

      while (packets<packetsNeeded) {
         try {
            // sending data in packets of 65507
            byte data[] = new byte[65507];


            //Generate the data used for the packet
            int size = 0;
            while (size < 65507 && fis.available() != 0) {
               data[size] = (byte) fis.read();
               size++;
            }
            if (size != 65507)
               size--;


            //array of senderThread (thread that manage the process of sending the file to a client and resending if necessary)
            senderThread[] threads = new senderThread[noc];
            for(int i = 0; i< noc; i++) {
               //create a new thread and start it
               threads[i] = new senderThread(server, addresses[i], cl_ports[i], size, data, packets, i);
               threads[i].run();
            }
            for(int i = 0; i< noc; i++) {
               //once every thread is launched wait for them to finish before proceeding
               threads[i].join();
               System.out.println("finished waiting for client " + i);
               packetsSent[i] += threads[i].getNBPacketSent();
               bytesSend[i] += threads[i].getNBByteSent();
            }

            packets++;
            System.out.println("All clients have received the packet "+ (packets) + "\n");
            
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

      server.close();//we don't need it anymore
      System.out.println("Sending file completed closing socket.");

      //stats
      int totalPacketSent = 0;
      for (int i: packetsSent) {
         totalPacketSent += i;
      }

      long totalByteSent = 0;
      for (long i : bytesSend) {
         totalByteSent += i;
      }

      System.out.println("\nStats :\n\tGlobal :");
      System.out.println("\t\t- Total packet sent to all (" + noc + ") clients : " + totalPacketSent);
      System.out.println("\t\t- Total byte sent to all (" + noc + ") clients : " + totalByteSent);

      System.out.println("\n\tBy client :");
      for(int i = 0; i<noc; i++) {
         float estimatedProbFail = (1-((float)packetsNeeded / packetsSent[i]))*100;
         System.out.println("\t\t- Client no " + i + ":");
         System.out.println("\t\t\t- Packets sent : " + packetsSent[i]);
         System.out.println("\t\t\t- Bytes sent : " + bytesSend[i]);
         System.out.println("\t\t\t- Estimated Probability of Faillure : " + estimatedProbFail);
      }


   }
}
