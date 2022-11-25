// server.java

import java.net.*;
import java.io.*;

public class server {
   public static void main(String [] args) throws IOException
   {
      int rcv_port = Integer.parseInt(args[0]);
      DatagramSocket server=new DatagramSocket(rcv_port);
	   DatagramPacket packet = null;
      int packets=0; //no of packets
      System.out.println("Server started!");
      byte[] buffer= new byte[256];
      //receiving file name
	   packet=new DatagramPacket(buffer, buffer.length);
      server.receive(packet);
      String fileName=new String(packet.getData(),0,packet.getLength());

      System.out.println("Client wants the file: "+fileName);
      // making file input stream(to read from it)
      FileInputStream fis= new FileInputStream(new File(fileName));
      int packetsNeeded=fis.available()/256+1; //nr of packets needed

      //send nr of packets needed
      buffer = Integer.toString(packetsNeeded).getBytes();
      InetAddress address = packet.getAddress();
      int rep_port = packet.getPort();
	   packet = new DatagramPacket(buffer, buffer.length,address,rep_port);
      server.send(packet);

      

      while(true)
      {
      try
      { //sending data in packets of 256
      byte data[]=new byte[256];
      int i=0;
      while(i<256&&fis.available()!=0)
         {data[i]=(byte)fis.read();
         int cha=(int)data[i];
         System.out.print((char) cha);
         i++;}
      if(i!=256) i--;
      //sending the packet length 
      byte lengthdata[]= Integer.toString(i).getBytes();
      packet=new DatagramPacket(lengthdata, lengthdata.length,address,rep_port);
      server.send(packet);

	    packet = new DatagramPacket(data, i,address,rep_port);
	    server.send(packet);
	   System.out.println("Packet no. "+ ++packets +" sent!\nContained:"+  new String(data).substring(0, i));
         if(packets==packetsNeeded)
            {
               server.close();
	         System.out.println("Connection closed");
             break;}
	   
                     

      } catch(SocketTimeoutException s)
         {
            System.out.println("Socket timed out!");
	    break;
	 }
      catch(IOException e)
         {
            e.printStackTrace();
	    break;
         }
     }
     
     }
}
