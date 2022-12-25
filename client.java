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
		long bytesReceived = 0;
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

		// int length; // length of the packet to be received

		while (true) {
			try {
				int chance = 100; // default chance is 100 will be overwriten because it is used
				long seqNr;
				Random rand = new Random(); // random for simulating failure
				byte[] seqbytes = new byte[10];// for signaling to the server that we received the packet or not

				// reset the data buffer each time
				byte[] data = new byte[65507];

				// receive packet
				packet = new DatagramPacket(data, data.length);
				client.receive(packet);

				for (int i = 0; i < 10 && packet.getData()[i] != '_'; i++)
					seqbytes[i] = packet.getData()[i];

				seqNr = Long.parseLong(new String(seqbytes).trim());

				System.out.println("Packet " + packets + "[Before addition]Seq : " + seqNr + "Ack: " + ackNumber);

				// simulating a fail if chance <=probFail
				chance = rand.nextInt(99) + 1; // formula for rng between range "generateRandom(max - min)+min"

				// we will send now the ack number
				// if the packet fails as the old one
				// otherwise adding the length of packet received
				if (chance < probFail || ackNumber != seqNr) { // in case of failure (reminder probFail is provided as
																// argument)

					System.out.println("From client:Packet " + packets + "failed for client " + noProcess);
					// send to server we are not good
					// send to server the ack number as well as the packet we are receiving this for
					seqbytes = new byte[10];
					seqbytes = Long.toString(ackNumber).getBytes();

					byte packetNumber[] = new byte[10];
					packetNumber = Integer.toString(packets).getBytes();

					byte[] response = new byte[21];

					// add the ack nr to the packet
					System.arraycopy(seqbytes, 0, response, 0, seqbytes.length);
					for (int i = seqbytes.length; i < 10; i++)
						response[i] = '_';

					// add the packet number to the packet
					System.arraycopy(packetNumber, 0, response, 10, packetNumber.length);
					for (int i = 10 + packetNumber.length; i < 20; i++)
						response[i] = '_';

					// add the client nr to the packet
					response[20] = (byte) noProcess;

					packet = new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
					client.send(packet);

					continue;
				}

				// if all is good the ack nr increases
				ackNumber += data.length - 10;

				System.out.println(
						"From client: Packet no. " + packets + " received by client" + noProcess + "!");

				// send to server the ack number as well as the packet we are receiving this for
				seqbytes = new byte[10];
				seqbytes = Long.toString(ackNumber).getBytes();

				byte packetNumber[] = new byte[10];
				packetNumber = Integer.toString(packets).getBytes();

				byte[] response = new byte[21];

				// add the ack nr to the packet
				System.arraycopy(seqbytes, 0, response, 0, seqbytes.length);
				for (int i = seqbytes.length; i < 10; i++)
					response[i] = '_';

				// add the packet number to the packet
				System.arraycopy(packetNumber, 0, response, 10, packetNumber.length);
				for (int i = 10 + packetNumber.length; i < 20; i++)
					response[i] = '_';

				// add the client nr to the packet
				response[20] = (byte) noProcess;

				packet = new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
				client.send(packet);

				// how about we try to get rid of this !

				bytesReceived += data.length;
				packets++;
				// writing to buffer the packet 0 and flushing it
				FOS.write(data, 10, data.length - 10);
				FOS.flush();

				if (packets == packetsNeeded) {
					// sending nr of bytes received(real of the file)
					byte[] signal = new byte[100];
					signal = Long.toString(bytesReceived).getBytes();
					packet = new DatagramPacket(signal, signal.length, serverName, port);
					client.send(packet);

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
