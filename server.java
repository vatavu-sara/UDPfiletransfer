// server.java

import java.net.*;
import java.io.*;

public class server {
   public static void main(String[] args) throws IOException {
      if(args.length!=2) {
			System.out.println("Arguments not valid!(Need 2)");
			System.exit(0);
		}
      int rcv_port = Integer.parseInt(args[0]);
      DatagramSocket server = new DatagramSocket(rcv_port);
      System.out.println("Server started!");
      DatagramPacket packet = null;
      int noc = Integer.parseInt(args[1]); // number of clients
      InetAddress addresses[] = new InetAddress[11];
      int cl_ports[] = new int[11];

      while (true) {

         int packets = 1; // no of packets sent for each file
         long bytesSend = 0;
         int packetsSent = 0;
         int received;
         byte[] buffer = new byte[10];

         // connecting to the clients
         for (int i = 0; i < noc; i++) {
            packet = new DatagramPacket(buffer, buffer.length);
            server.receive(packet);
            int position = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
            addresses[position] = packet.getAddress();
            cl_ports[position] = packet.getPort();

         }

         // sending acknowledge we are connected to all clients
         for (int i = 1; i <= noc; i++) {
            buffer = new byte[10];
            buffer = Integer.toString(1).getBytes();
            packet = new DatagramPacket(buffer, buffer.length, addresses[i], cl_ports[i]);
            server.send(packet);
         }

         // receiving file name from the first*(not necessarily in order) process
         buffer = new byte[100];
         packet = new DatagramPacket(buffer, buffer.length);
         server.receive(packet);
         String fileName = new String(packet.getData(), 0, packet.getLength());
         String fn2;

         // receiving file name from the others too
         for (int i = 1; i < noc; i++) {
            packet = new DatagramPacket(buffer, buffer.length);
            server.receive(packet);
            fn2 = new String(packet.getData(), 0, packet.getLength());
            // if (fn2 != fileName) {
            //    System.out.println("Clients want different files :(");
            //    server.close();
            //    System.exit(0);
            // }
         }

         System.out.println("Clients wants the file: " + fileName);

         // making file input stream(to read from it)
         FileInputStream fis = new FileInputStream(new File(fileName));

         // max 65507 ?
         int packetsNeeded = fis.available() / 65507 + 1; // nr of packets needed for each

         for (int i = 1; i <= noc; i++) {
            // send nr of packets needed
            buffer = new byte[100];
            buffer = Integer.toString(packetsNeeded).getBytes();

            System.out.println("Address: " + addresses[i] + " Port: " + cl_ports[i]);
            packet = new DatagramPacket(buffer, buffer.length, addresses[i], cl_ports[i]);
            server.send(packet);
         }

         while (true) {
            try { DatagramPacket packetLength= null;
               DatagramPacket status=null;
               DatagramPacket bytes=null;
               // sending data in packets of 65507
               byte data[] = new byte[65507];
   

               int size = 0;
               while (size < 65507 && fis.available() != 0) {
                  data[size] = (byte) fis.read();
                  size++;
               }
               if (size != 65507)
                  size--;

               // fis.read(data);
               // packet length

               for (int i = 1; i <= noc; i++) {
                  byte buffer2[] = new byte[10];
               
               //send packet length
               buffer2 = Integer.toString(size).getBytes();
               packetLength = new DatagramPacket(buffer2, buffer2.length,addresses[i],cl_ports[i]);
               server.send(packetLength);
               

               // sending the packet itself
               packet = new DatagramPacket(data, size,addresses[i],cl_ports[i]);
               server.send(packet);

                  // receive status of packet (1 received 0 failed)
                  status = new DatagramPacket(buffer2, buffer2.length);
                  server.receive(status);
                  received = Integer.parseInt(new String(status.getData(), 0, status.getLength()));

                  packetsSent++;
                  bytesSend += size;
                  while (received == 0) { 
                     buffer2 = new byte[10];

                     // sending the packet length
                     buffer2 = Integer.toString(size).getBytes();
                     packetLength = new DatagramPacket(buffer2, buffer2.length,addresses[i],cl_ports[i]);
                     server.send(packetLength);
                     
                     // sending the packet itself
                     packet = new DatagramPacket(data, size,addresses[i],cl_ports[i]);
                     server.send(packet);

                     // receive status of packet
                     status = new DatagramPacket(buffer2, buffer2.length);
                     server.receive(status);
                     received = Integer.parseInt(new String(status.getData(), 0, status.getLength()));
                     packetsSent++;
                     bytesSend += size;
                  }

                  // packets++;

                  System.out.println("Packet no. " + packets + " sent to client" +i+ "!Length:" + size);

                  if (packets == packetsNeeded) {
                     // sending total bytes sent
                     buffer = Long.toString(bytesSend).getBytes();
                     bytes = new DatagramPacket(buffer, buffer.length, addresses[i], cl_ports[i]);
                     server.send(bytes);

                     // sending total packets sent
                     buffer = Integer.toString(packetsSent).getBytes();
                     bytes = new DatagramPacket(buffer, buffer.length, addresses[i], cl_ports[i]);
                     server.send(bytes);
                     System.out.println("Client "+i +"  fully received the file!");
                     // server.close();
                     // System.out.println("Connection closed");
                     
                  }
               } packets++;
               System.out.println("All clients have received the packet "+(packets-1));
               if(packets==packetsNeeded+1)
               break;   
               
            } catch (SocketTimeoutException s) {
               System.out.println("Socket timed out!");
               break;
            } catch (IOException e) {
               e.printStackTrace();
               break;
            }
         }
      }

   }
}
