// client.java

import java.net.*;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.Random;

public class client {

	public static void main(String[] args) throws IOException {
		// String serverName = args[0];
		if(args.length!=3) {
			System.out.println("Arguments not valid!(Need 3)");
			System.exit(0);
		}
		InetAddress serverName = InetAddress.getLocalHost();
		int port = Integer.parseInt(args[0]);
		int noProcess= Integer.parseInt(args[1]);
		String dirName="clients/client"+noProcess; //create directory client1..
		new File(dirName).mkdir();



		String newfilepath =dirName+"/received"; // path of new file
		int probFail = Integer.parseInt(args[2]); //probability % of simulating fail
		int packets = 1; // nr of packets received
		int packetsSent;
		int bytesReceived=0;


		FileOutputStream FOS = new FileOutputStream(newfilepath);
		DatagramSocket client = new DatagramSocket();
		DatagramPacket packet;

		// send number of process wanted to server
		byte[] buffer = new byte[10];
		buffer = Integer.toString(noProcess).getBytes();
		packet = new DatagramPacket(buffer, buffer.length, serverName, port);
		client.send(packet);

		//receive ack that all clients are connected
		packet = new DatagramPacket(buffer, buffer.length);
		client.receive(packet);
		System.out.println(new String(packet.getData(), 0, packet.getLength()));
		System.out.println("Hello there client! All clients are now connected to the server");

		
		// send which file wanted to server = move to sv
		/*buffer = new byte[100];
		buffer = filename.getBytes();
		packet = new DatagramPacket(buffer, buffer.length, serverName, port);
		client.send(packet);*/

		

		

		// receive no of packets needed
		buffer = new byte[10]; //fixed issue by recreating a new buffer for sending the numbers of packets
	
		packet = new DatagramPacket(buffer, buffer.length);
		client.receive(packet);
		int packetsNeeded = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
	
		System.out.println("Your file will come in " + packetsNeeded + " packets!");

		while (true) {
			try {
				// receiving file packets
				byte[] data = new byte[65507];
				byte[] buffer2 = new byte[10];

				// receive lenght of new packet
				packet = new DatagramPacket(buffer2, buffer2.length);
				System.out.println("awaiting size of new packet...");
				client.receive(packet);
				int length = Integer.parseInt(new String(packet.getData(),0,packet.getLength()));
				System.out.println("New packet length = " + length);

				//receive packet		
				packet = new DatagramPacket(data, length);
				client.receive(packet);

				//simulating a fail if chance <probFail 
				Random rand= new Random();
				int chance = rand.nextInt(100);
				
				

				while(chance < probFail ) {
					byte[] buffer3 = new byte[10];
					
					System.out.println("Packet "+ packets +" failed to receive with "+ chance +"/" +probFail);

					//sending 0 for resend
					buffer2 = new byte[10];
					buffer2= Integer.toString(0).getBytes();
					packet = new DatagramPacket(buffer2, buffer2.length, serverName, port);
					client.send(packet);

					// receive lenght of new packet
					packet = new DatagramPacket(buffer3, buffer3.length);
					client.receive(packet);
					length = Integer.parseInt(new String(packet.getData(),0,packet.getLength()));

					//receive packet		
					packet = new DatagramPacket(data, length);
					client.receive(packet);

					//simulating a fail if chance <=probFail
					chance = rand.nextInt(100);

				}

				//sending 1 for go forward
				buffer2= Integer.toString(1).getBytes();
               	packet = new DatagramPacket(buffer2, buffer2.length, serverName, port);
               	client.send(packet);
				bytesReceived+=length;
				//packets++;
				System.out.println("Packet no. " + packets++ + " received!Length: "+length);


				FOS.write(data,0,length);
				FOS.flush();
				System.out.println("next...");

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
