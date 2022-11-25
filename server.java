// server.java

import java.net.*;
import java.io.*;

public class server {
   public static void main(String[] args) throws IOException {
      int rcv_port = Integer.parseInt(args[0]);
      DatagramSocket server = new DatagramSocket(rcv_port);
      DatagramPacket packet = null;
      int packets = 0; // no of packets
      int sent, received;
      System.out.println("Server started!");
      byte[] buffer = new byte[512];
      // receiving file name
      packet = new DatagramPacket(buffer, buffer.length);
      server.receive(packet);
      String fileName = new String(packet.getData(), 0, packet.getLength());

      System.out.println("Client wants the file: " + fileName);

      // making file input stream(to read from it)
      FileInputStream fis = new FileInputStream(new File(fileName));
      int packetsNeeded = fis.available() / 5112 + 1; // nr of packets needed

      // send nr of packets needed
      buffer = Integer.toString(packetsNeeded).getBytes();
      InetAddress address = packet.getAddress();
      int rep_port = packet.getPort();
      packet = new DatagramPacket(buffer, buffer.length, address, rep_port);
      server.send(packet);

      while (true) {
         try { // sending data in packets of 512
            byte data[] = new byte[512];
            int i = 0;
            while (i < 512 && fis.available() != 0) {
               data[i] = (byte) fis.read();
               i++;
            }
            if (i != 512)
               i--;

            // fis.read(data);
            // sending the packet length
            buffer = Integer.toString(i).getBytes();
            packet = new DatagramPacket(buffer, buffer.length, address, rep_port);
            server.send(packet);

            // sending the packet itself
            packet = new DatagramPacket(data, i, address, rep_port);
            server.send(packet);

            // sending number of packet
            sent = packets;
            buffer = Integer.toString(sent).getBytes();
            packet = new DatagramPacket(buffer, buffer.length, address, rep_port);
            server.send(packet);

            // receive ack of packet
            packet = new DatagramPacket(buffer, buffer.length);
            server.receive(packet);
            received = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));

            while (sent != received) {
               // send notOK message
               buffer = "notOK".getBytes();
               packet = new DatagramPacket(buffer, buffer.length, address, rep_port);
               server.send(packet);

               // sending the packet length
               buffer = Integer.toString(i).getBytes();
               packet = new DatagramPacket(buffer, buffer.length, address, rep_port);
               server.send(packet);

               // sending the packet itself
               System.out.println("Packet "+ packets +" failed to receive!Retrying..");
               packet = new DatagramPacket(data, i, address, rep_port);
               server.send(packet);

               // sending number of packet
               sent = packets;
               buffer = Integer.toString(sent).getBytes();
               packet = new DatagramPacket(buffer, buffer.length, address, rep_port);
               server.send(packet);

               // receive ack of packet
               packet = new DatagramPacket(buffer, buffer.length);
               server.receive(packet);
               received = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
            }

            // send ok message
            buffer = "OK".getBytes();
            packet = new DatagramPacket(buffer, buffer.length, address, rep_port);
            server.send(packet);

            System.out.println("Packet no. " + ++packets + " sent!Length:" + i + "\nContained:" + new String(data));


            if (packets == packetsNeeded) {
               server.close();
               System.out.println("Connection closed");
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
