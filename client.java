// client.java

import java.net.*;
import java.io.*;

public class client {

	void getPacket()
	{}
	public static void main(String[] args) throws IOException {
		// String serverName = args[0];
		InetAddress serverName = InetAddress.getLocalHost();
		int port = Integer.parseInt(args[0]);
		String filename = args[1]; // name of file wanted
		String newfilepath = args[2]; // path of new file
		int packets = 1; // nr of packets received
		int received;
		System.out.println("Hello there client!");

		FileOutputStream FOS = new FileOutputStream(newfilepath);
		DatagramSocket client = new DatagramSocket();
		DatagramPacket packetdata = null;
		DatagramPacket nos=null;

		// send which file wanted to server
		byte[] buffer = new byte[512];
		buffer = filename.getBytes();
		nos = new DatagramPacket(buffer, buffer.length, serverName, port);
		client.send(nos);

		// receive no of packets needed
		nos = new DatagramPacket(buffer, buffer.length);
		client.receive(nos);
		int packetsNeeded = Integer.parseInt(new String(nos.getData(), 0, nos.getLength()));
		System.out.println("Your file will come in " + packetsNeeded + " packets!");

		while (true) {
			try {
				// receiving file packets
				byte[] data = new byte[512];
				byte[] buffer2 = new byte[512];
				

				// receive lenght of new packet
				packetdata = new DatagramPacket(buffer2, buffer2.length);
				client.receive(packetdata);
				int length = Integer.parseInt(new String(packetdata.getData(),0,packetdata.getLength()));

				//receive packet		
				packetdata = new DatagramPacket(data, length);
				client.receive(packetdata);

				//receive number of packet 
				nos = new DatagramPacket(buffer2, buffer2.length);
				client.receive(nos);
				received = Integer.parseInt(new String(nos.getData(), 0, nos.getLength()));
				

				//send acknowledge of packet 
				buffer2= Integer.toString(received).getBytes();
				nos=new DatagramPacket(buffer2, buffer2.length,serverName,port);
				client.send(nos);
				

				//receive ok/notok
				// nos = new DatagramPacket(buffer, buffer.length);
				// client.receive(nos);

				while(received!=packets){
				byte[] buffer3 = new byte[512];

				// receive lenght of new packet
				packetdata = new DatagramPacket(buffer2, buffer2.length);
				client.receive(packetdata);
				length = Integer.parseInt(new String(packetdata.getData(),0,packetdata.getLength()));

				//receive packet		
				packetdata = new DatagramPacket(data, length);
				client.receive(packetdata);

				//receive number of packet 
				nos = new DatagramPacket(buffer3, buffer3.length);
				client.receive(nos);
				received = Integer.parseInt(new String(nos.getData(), 0, nos.getLength()));

				//send acknowledge of packet 
				buffer3= Integer.toString(received).getBytes();
				nos=new DatagramPacket(buffer3, buffer3.length,serverName,port);
				client.send(nos);

				//receive ok/notok
				// nos = new DatagramPacket(buffer2, buffer2.length);
				// client.receive(nos);
	

				}
				//System.out.println("Packet no. " + packets++ + " received!Length: "+length + "\nContained: " + new String(packetdata.getData()));


				FOS.write(data,0,length);
				FOS.flush();

				if (packets > packetsNeeded) {
					client.close();
					FOS.close();
					System.out.println("Done. Socket closed");
					break;
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
