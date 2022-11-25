// client.java

import java.net.*;
import java.io.*;

public class client {
	public static void main(String[] args) throws IOException {
		// String serverName = args[0];
		InetAddress serverName = InetAddress.getLocalHost();
		int port = Integer.parseInt(args[0]);
		String filename = args[1]; // name of file wanted
		String newfilepath = args[2]; // path of new file
		int packets = 0; // nr of packets received
		System.out.println("Hello there client!");

		FileOutputStream FOS = new FileOutputStream(newfilepath);
		DatagramSocket client = new DatagramSocket();
		DatagramPacket packet = null;

		// send which file wanted to server
		byte[] buffer = new byte[256];
		buffer = filename.getBytes();
		packet = new DatagramPacket(buffer, buffer.length, serverName, port);
		client.send(packet);

		// receive no of packets needed
		packet = new DatagramPacket(buffer, buffer.length);
		client.receive(packet);
		int packetsNeeded = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
		System.out.println("Your file will come in " + packetsNeeded + " packets!");

		while (true) {
			try {// receiving file packets
				byte[] data = new byte[256];

				// receive lenght of new packet
				packet = new DatagramPacket(buffer, buffer.length);
				client.receive(packet);
				int length = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
			
				//receive packet		
				packet = new DatagramPacket(data, length);
				client.receive(packet);
				FOS.write(packet.getData());
				System.out.println("Packet no. " + ++packets + " received!Length: "+length + "\nContained: " + new String(packet.getData()));

				if (packets == packetsNeeded) {
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
