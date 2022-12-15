// client.java

import java.net.*;
import java.io.*;
import java.util.Random;

public class client extends Thread {
	private String[] args;

	public static void main(String[] args) throws IOException {
		// String serverName = args[0];
		if (args.length != 4) {
			System.out.println("Arguments not valid!(Need 3)");
			System.exit(0);
		}
		InetAddress serverName = InetAddress.getLocalHost();
		int port = Integer.parseInt(args[0]);
		int noc = Integer.parseInt(args[1]); // nr of clients
		int noProcess = Integer.parseInt(args[2]); // its index
		String dirName = "clients/client" + noProcess; // create directory client1..
		new File(dirName).mkdir();

		int probFail = Integer.parseInt(args[3]); // probability % of simulating fail
		int packets = 0; // nr of packets received
		long ackNumber = 1;

		DatagramSocket client = new DatagramSocket();
		DatagramPacket packet;

		int[] portsClients = new int[10];

		// send number of process wanted to server
		byte[] buffer = new byte[10];
		buffer = Integer.toString(noProcess).getBytes();
		packet = new DatagramPacket(buffer, buffer.length, serverName, port);
		client.send(packet);

		// receive ports of other clients(including himself for ease)
		for (int i = 0; i < noc; i++) {
			buffer = new byte[6];
			packet = new DatagramPacket(buffer, buffer.length);
			client.receive(packet);
			portsClients[i] = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
		}

		buffer = new byte[100];
		// receive ack that all clients are connected(as the filename)
		packet = new DatagramPacket(buffer, buffer.length);
		client.receive(packet);
		System.out.println("Hello there client! All clients are now connected to the server");
		String newfilepath = dirName + "/" + new String(packet.getData(), 0, packet.getLength()); // path of new file
		FileOutputStream FOS = new FileOutputStream(newfilepath);

		// receive no of packets needed
		buffer = new byte[10]; // fixed issue by recreating a new buffer for sending the numbers of packets

		packet = new DatagramPacket(buffer, buffer.length);
		client.receive(packet);
		int packetsNeeded = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));

		System.out.println("Your file will come in " + packetsNeeded + " packets!");

		int length; // length of the packet to be received

		while (true) {
			try {
				int chance = 100; // default chance is 100 will be overwriten because it is used
				long seqNr;
				Random rand = new Random(); // random for simulating failure
				byte[] signal = new byte[10];// for signaling to the server that we received the packet or not

				// might also be optimized with do while but not priority at the moment
				byte[] packetLen = new byte[10]; // packLen -> packet with the len of the other packet; not to be
													// confused with length tho packLen give the value of length

				// reset the data buffer each time
				byte[] data = new byte[65507];

				// receive seqnr of new packet
				packet = new DatagramPacket(packetLen, packetLen.length);
				client.receive(packet);
				seqNr = Long.parseLong(new String(packet.getData(), 0, packet.getLength()));

				// receive lenght of new packet
				packet = new DatagramPacket(packetLen, packetLen.length);
				client.receive(packet);
				length = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
				// System.out.println("Got the size:"+length);

				// receive packet
				packet = new DatagramPacket(data, length);
				client.receive(packet);

				int portR = packet.getPort();

				// simulating a fail if chance <=probFail
				chance = rand.nextInt(99) + 1; // formula for rng between range "generateRandom(max - min)+min"
				
				if (chance < probFail) { // in case of failure (reminder probFail is provided as
																// argument)


					//send to server we are not good
					signal = new byte[2];
					signal = Integer.toString(0).getBytes();
					packet = new DatagramPacket(signal, signal.length, serverName, portR);
					client.send(packet);
					
					continue;
				}

				ackNumber += length;

				System.out.println(
						"Packet no. " + packets + " received by client" + noProcess + "!");

				

				//send to server we are ALL good
				signal = new byte[2];
				signal = Integer.toString(1).getBytes();
				packet = new DatagramPacket(signal, signal.length, serverName, portR);
				client.send(packet);
	
				//waiting for signal from server that all is good
				do{
				packetLen = new byte[2];
				packet = new DatagramPacket(packetLen, packetLen.length);
				client.receive(packet);
				length = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
				}while(length==0);

				packets++;

				// writing to buffer the packet 0 and flushing it
				FOS.write(data, 0, length);
				FOS.flush();

				if (packets == packetsNeeded) {
					client.close();
					FOS.close();
					System.out.println("C" + noProcess
							+ ":Done transmiting. Socket closed\nYou may observe the resulting file in " + dirName);
					break;// breaks out of the while loop as we have finished
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// to run the client
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
