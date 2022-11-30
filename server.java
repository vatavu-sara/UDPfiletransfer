// server.java

import java.net.*;
import java.io.*;

public class server {
   public static void main(String[] args) throws IOException {
      int rcv_port = Integer.parseInt(args[0]);
      DatagramSocket server = new DatagramSocket(rcv_port);
      System.out.println("Server started!");
      

      while(true){
      DatagramPacket packet = null;
      int packets = 1; // no of packets
      long bytesSend=0;
      int packetsSent=0;
      int sent, received;
      byte[] buffer = new byte[100];
      // receiving file name
      packet = new DatagramPacket(buffer, buffer.length);
      server.receive(packet);
      String fileName = new String(packet.getData(), 0, packet.getLength());

      System.out.println("Client wants the file: " + fileName);

      // making file input stream(to read from it)
      FileInputStream fis = new FileInputStream(new File(fileName));

      //max 65507 ?
      int packetsNeeded = fis.available() / 65507 + 1; // nr of packets needed

      // send nr of packets needed
      buffer = Integer.toString(packetsNeeded).getBytes();
      InetAddress address = packet.getAddress();
      int rep_port = packet.getPort();
      packet = new DatagramPacket(buffer, buffer.length, address, rep_port);
      server.send(packet);

      while (true) {
         try { // sending data in packets of 65507
            byte data[] = new byte[65507];
            byte buffer2[] = new byte[10];

            int i = 0;
            while (i < 65507 && fis.available() != 0) {
               data[i] = (byte) fis.read();
               i++;
            }
            if (i != 65507)
               i--;

            // fis.read(data);
            // sending the packet length
            buffer2 = Integer.toString(i).getBytes();
            packet = new DatagramPacket(buffer2, buffer2.length, address, rep_port);
            server.send(packet);

            // sending the packet itself
            packet = new DatagramPacket(data, i, address, rep_port);
            server.send(packet);

            // receive status of packet (1 received 0 failed)
            packet = new DatagramPacket(buffer2, buffer2.length);
            server.receive(packet);
            received = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));

            packetsSent++;
            bytesSend+=i;
            while (received==0) {
            byte[] buffer3 = new byte[10];

               // sending the packet length
               buffer3 = Integer.toString(i).getBytes();
               packet = new DatagramPacket(buffer3, buffer3.length, address, rep_port);
               server.send(packet);

               // sending the packet itself
               System.out.println("Packet "+ packets +" failed to receive!Retrying..");
               packet = new DatagramPacket(data, i, address, rep_port);
               server.send(packet);

               // receive status of packet
               packet = new DatagramPacket(buffer2, buffer2.length);
               server.receive(packet);
               received = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
               packetsSent++;

               bytesSend+=i;
            }

            packets++;
            //System.out.println("Packet no. " + packets++ + " sent!Length:" + i );


            if (packets > packetsNeeded) {
               //sending total bytes sent
               buffer=Long.toString(bytesSend).getBytes();
               packet=new DatagramPacket(buffer, buffer.length,address,rep_port);
               server.send(packet);

               //sending total packets sent
               buffer=Integer.toString(packetsSent).getBytes();
               packet=new DatagramPacket(buffer, buffer.length,address,rep_port);
               server.send(packet);
               System.out.println("Client received the file " +fileName);
               // server.close();
               // System.out.println("Connection closed");
               break;
            }

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
