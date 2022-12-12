// client.java

import java.net.*;
import java.io.*;
import java.util.Random;

public class client extends Thread{
	private String[] args;

	public static void main(String[] args) throws IOException {
		// String serverName = args[0];
		if(args.length!=3) {
			System.out.println("Arguments not valid!(Need 3)");
			System.exit(0);
		}
		InetAddress serverName = InetAddress.getLocalHost();
		int port = Integer.parseInt(args[0]);
		int noProcess= Integer.parseInt(args[1]); //to start from 0
		String dirName="clients/client"+noProcess; //create directory client1..
		new File(dirName).mkdir();



		String newfilepath =dirName+"/received"; // path of new file
		int probFail = Integer.parseInt(args[2]); //probability % of simulating fail
		int packets = 1; // nr of packets received


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
		System.out.println("Hello there client! All clients are now connected to the server");
		

		// receive no of packets needed
		buffer = new byte[10]; //fixed issue by recreating a new buffer for sending the numbers of packets
	
		packet = new DatagramPacket(buffer, buffer.length);
		client.receive(packet);
		int packetsNeeded = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
	
		//System.out.println("Your file will come in " + packetsNeeded + " packets!");


		while (true) {
			try {
				int chance = 100; //default chance is 100 will be overwriten because it is used 
				int length = 0; //length of the packet to be received
				Random rand = new Random(); // random for simulating failure
				byte[] signal = new byte[10];//for signaling to the server that we received the packet or not
				byte[] data = new byte[65507]; 

				do {// might also be optimized with do while but not priority at the moment
					byte[] packetLen = new byte[10]; //packLen -> packet with the len of the other packet; not to be confused with length tho packLen give the value of length
					
					data = new byte[65507]; //reset the data buffer each time
					

					// receive lenght of new packet
					packet = new DatagramPacket(packetLen, packetLen.length);
					client.receive(packet);
					length = Integer.parseInt(new String(packet.getData(),0,packet.getLength()));
			

					//receive packet		
					packet = new DatagramPacket(data, length);
					client.receive(packet);

			
					//simulating a fail if chance <=probFail
					chance = rand.nextInt(99) + 1; //formula for rng between range "generateRandom(max - min)+min"

					if(chance < probFail) { //in case of failure (reminder probFail is provided as argument)
				
						System.out.println("Packet "+ packets +" failed to receive with "+ chance +"/" +probFail +"in client"+ noProcess);
						//add a sleep as timeout
						try {
							//sleep(100);
						} catch (Exception e) {
							// TODO: handle exception
						}
						//sending 0 for resend
						signal = new byte[10];
						signal = Integer.toString(0).getBytes();
						packet = new DatagramPacket(signal, signal.length, serverName, port);
						client.send(packet);
						

					} 
					else
					{break;}

				}while(true);

				//sending 1 for go forward
				signal = new byte[10];
				signal= Integer.toString(1).getBytes();
               	packet = new DatagramPacket(signal, signal.length, serverName, port);
               	client.send(packet);

				System.out.println("Packet no. " + packets + " received"+" (c"+noProcess+") "+"; !Length: "+length + "\n");
				packets++;

				//writing to buffer and flushing it
				FOS.write(data,0,length);
				FOS.flush();

				if (packets > packetsNeeded) {
					client.close();
					FOS.close();
					System.out.println("C"+noProcess+":Done transmiting. Socket closed\nYou may observe the resulting file in " + dirName);
					break;//breaks out of the while loop as we have finished
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	   //to run the client
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
