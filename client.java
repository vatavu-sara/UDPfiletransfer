// client.java

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class client extends Thread {
	static int MAX_PKT_SZ = 65507; // in bytes
	private String[] args;

	public static void main(String[] args) throws IOException {
		// String serverName = args[0];
		if (args.length != 5) {
			System.out.println("Arguments not valid!(Need 5)");
			System.exit(0);
		}
		InetAddress serverName = InetAddress.getLocalHost();
		int port = Integer.parseInt(args[0]);
		int noc = Integer.parseInt(args[1]); // nr of clients
		int noProcess = Integer.parseInt(args[2]); // its index
		String dirName = "clients/client" + noProcess; // create directory client1..
		new File("clients").mkdir(); // create directory "clients" if not existent
		new File(dirName).mkdir();

		int probFail = Integer.parseInt(args[3]); // probability % of simulating fail
		int windowSize = Integer.parseInt(args[4]);
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

		// this will be the point in the window from where the client started missing
		// packets
		int missingPoint = 0; //as first index at the start

		while (true) {
			try {

				// we want to receive only packets in our window
				long minseq = ackNumber; // supposedly we already received all the packets up to the window position
				long maxseq = ackNumber + windowSize * MAX_PKT_SZ;
				ArrayList<DatagramPacket> packetsReceived = new ArrayList<DatagramPacket>(windowSize);
				ArrayList<Long> seqNrs = new ArrayList<Long>(windowSize);

				// receive how many packets actually got received by everybody!
				buffer = new byte[5];
				packet = new DatagramPacket(buffer, buffer.length);
				client.receive(packet);
				int packetsReceivedByAll = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));

				int receivedAlready = missingPoint; // how many this client already received

				// this will be then the new starting point to send again the windowsize as the
				// window
				// is advanced by packetsReceivedByAll
				missingPoint = missingPoint - packetsReceivedByAll;

				// if there are less packets left than the WS we need to decrease it
				if (packets - missingPoint >= packetsNeeded - windowSize) {
					windowSize = packetsNeeded - packets;

				}

				System.out.println("All clients received " + packetsReceivedByAll
						+ " packets from windowsize and client " + noProcess + " received " + receivedAlready
						+ "so starting from packet " + missingPoint + " in the window (" + packets + ") wsize:"
						+ windowSize);
				System.out.println("Client " + noProcess + " receiving packets " + packets + " - "
						+ (packets + windowSize - missingPoint - 1));
				long tmpack = ackNumber; // to add

				// need to know somehow where we actually start
				// eg if i need only packets 3-5 and ws is 6 i start from 3, not from 0

				for (int a = missingPoint; a < windowSize; a++) {
					System.out.println("a:" + a);
					int chance = 100; // default chance is 100 will be overwriten because it is used
					long seqNr;
					Random rand = new Random(); // random for simulating failure

					// reset the data buffer each time
					byte[] data = new byte[65507];

					// receive packet
					packet = new DatagramPacket(data, data.length);
					client.receive(packet);

					// receive seqnr
					seqNr = getSeq(packet);

					int indexPacket = (int) seqNr / 65507; // supposedly

					// receive size
					int size = getSize(packet);

					System.out.println("Client " + noProcess + " attempts to receive packet " + indexPacket + " length"
							+ packet.getLength());
					System.out.println("[Before addition]Seq : " + seqNr + "Ack: " + tmpack + " size:" + size);

					// simulating a fail if chance <=probFail
					chance = rand.nextInt(99) + 1; // formula for rng between range "generateRandom(max - min)+min"

					// if the packet fails(aka it gets lost) we need to "drop" it

					if (chance < probFail || seqNr < minseq || seqNr > maxseq) {
						// in case of failure (reminder probFail is provided as
						// argument)

						System.out.println(
								"Packet " + indexPacket + "FAAAAAILEDDDDDDDDDDD for client " + noProcess);

						continue;
					} else {
						System.out.println(
								"From client:Packet " + indexPacket + "RECEIVEDdddd for client " + noProcess);
						tmpack += size; // the tmpack increases :)
						packetsReceived.add(packet);
						seqNrs.add(seqNr);
					}

				}
				// sort the list of packets based on seqnr

				for (int i = 0; i < packetsReceived.size(); i++)
					for (int j = i + 1; j < packetsReceived.size(); j++)
						if (seqNrs.get(i) > seqNrs.get(j)) {// swap size
							long tempsq = seqNrs.get(i);
							seqNrs.set(i, seqNrs.get(j));
							seqNrs.set(j, tempsq);
							// swap packet itself
							DatagramPacket tempPck = packetsReceived.get(i);
							packetsReceived.set(i, packetsReceived.get(j));
							packetsReceived.set(j, tempPck);
						}

				// the maximum packets received is the size of them(if all got received
				// correctly)
				int tmppoint = missingPoint; //the index in the window
				missingPoint += packetsReceived.size(); //maximum missing point is the starting point + nr pcks received
				long lastpck = packets; //nr of last packet received

				for (int i = 0; i < packetsReceived.size(); i++)
					// in case the current packet is a forward to the necessary one we need to
					// break the cycle
					if (ackNumber != seqNrs.get(i)) {
						missingPoint = tmppoint + i;
						break;
					}
					// otherwise all is good for this packet so we can even write it and send an OK
					else {
						// the ack number ofc increases by the size of the packet
						ackNumber += getSize(packetsReceived.get(i));

						System.out.println(
								"From client: Packet no. " + packets + " written by client" + noProcess + "!");

						// send to server the ack number as well as the packet we are receiving this for
						byte[] seqbytes = new byte[10];
						seqbytes = Long.toString(ackNumber).getBytes();

						byte packetNumber[] = new byte[10];
						packetNumber = Long.toString(getSeq(packetsReceived.get(i)) / 65507).getBytes();
						lastpck = getSeq(packetsReceived.get(i)) / 65507 + 1;
						byte[] response = new byte[21];

						// add the ack nr to the packet
						System.arraycopy(seqbytes, 0, response, 0, seqbytes.length);
						for (int j = seqbytes.length; j < 10; j++)
							response[j] = '_';

						// add the packet number to the packet
						System.arraycopy(packetNumber, 0, response, 10, packetNumber.length);
						for (int j = 10 + packetNumber.length; j < 20; j++)
							response[j] = '_';

						// add the client nr to the packet
						response[20] = (byte) noProcess;

						packet = new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
						client.send(packet);

						//add the size of the packets to the counter of bytes received by this client
						bytesReceived += getSize(packetsReceived.get(i));

						//increasing the number of packets received 

						packets++;
						// writing to buffer the packet and flushing it
						FOS.write(packetsReceived.get(i).getData(), 15, getSize(packetsReceived.get(i)) - 15);
						FOS.flush();
					}

				// sending the same ack for the other packets
				for (int i = missingPoint; i < windowSize; i++) {

					System.out.println(
							"From client: Packet no. " + (packets + i - missingPoint) + " failed by client" + noProcess
									+ "!");

					// send to server the ack number as well as the packet we are receiving this for
					byte[] seqbytes = new byte[10];
					seqbytes = Long.toString(ackNumber).getBytes();

					byte packetNumber[] = new byte[10];
					packetNumber = Long.toString(lastpck++).getBytes();

					byte[] response = new byte[21];

					// add the ack nr to the packet
					System.arraycopy(seqbytes, 0, response, 0, seqbytes.length);
					for (int j = seqbytes.length; j < 10; j++)
						response[j] = '_';

					// add the packet number to the packet
					System.arraycopy(packetNumber, 0, response, 10, packetNumber.length);
					for (int j = 10 + packetNumber.length; j < 20; j++)
						response[j] = '_';

					// add the client nr to the packet
					response[20] = (byte) noProcess;

					packet = new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
					client.send(packet);
				}

				// deleting the packets well received/written
				for (int i = tmppoint; i < missingPoint; i++) { 
					packetsReceived.remove(0);
				}

				//when all the packets have been received and written to the client
				if (packets == packetsNeeded) {
					String msg;
					// maybe wait for signal that all finished
					do{
					buffer=new byte[2];
					packet = new DatagramPacket(buffer, buffer.length);
					client.receive(packet);
					msg= new String(packet.getData(), 0, packet.getLength());
					System.out.println(msg);
					
					}while(!msg.equals("OK"));

					// sending nr of bytes received(real of the file)
					byte[] signal = new byte[100];
					signal = Long.toString(bytesReceived).getBytes();
					packet = new DatagramPacket(signal, signal.length, serverName, port);
					client.send(packet);

					client.close();
					FOS.close();
					System.out.println("C" + noProcess
							+ ":Done transmiting.(" + bytesReceived
							+ "bytes) Socket closed\nYou may observe the resulting file in " + dirName);
					break;// breaks out of the while loop as we have finished
				}

			} catch (IOException e) {
				System.err.println("Smth wrong");
			}
		}
	}

	// to run the client
	public void setArgs(String[] args) {
		this.args = args;
	}

	public static long getSeq(DatagramPacket p) {
		byte seqbytes[] = new byte[10];
		for (int i = 0; i < 10 && p.getData()[i] != '_'; i++)
			seqbytes[i] = p.getData()[i];
		return Long.parseLong(new String(seqbytes).trim());

	}

	public static int getSize(DatagramPacket p) {
		byte sizeBytes[] = new byte[5];
		for (int i = 10; i < 15 && p.getData()[i] != '_'; i++)
			sizeBytes[i - 10] = p.getData()[i];
		return Integer.parseInt(new String(sizeBytes).trim());
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
