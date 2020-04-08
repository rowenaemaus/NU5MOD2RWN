package pckg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class UDProtocol {
	/*
	 * The first byte of the header contains the packet number, the
	 * second byte is eithe 0 (data) of 1 (ack), third byte is 1 (last file)
	 * or 0 (not last file)
	 */

	DatagramSocket socket = null;
	private InetAddress ownIP;
	private int ownPort;

	private InetAddress otherIP = null;
	private int otherPort = -1;

	private static final String MULTICAST = "230.0.0.0";
	private InetAddress multicastAddr;
	private int multicastPort = 5555;
	private int multicastSize;
	private String mcMsg1 = "hiserver";
	private String mcMsg2 = "hiclient";

	private static final int HEADERSIZE = 3; 
	private static final int DATASIZE = 128;

	Set<Integer> pktsSent = new HashSet<>();
	Set<Integer> pktsReceived = new HashSet<>();
	Set<Integer> acksReceived = new HashSet<>();

	String name;
	File fileDest;

	public UDProtocol(String name, int port) {
		try {
			this.ownIP = InetAddress.getLocalHost();
			this.ownPort = port;
			this.name = name;
			this.multicastAddr = InetAddress.getByName(MULTICAST);
			this.multicastSize = mcMsg1.getBytes().length;
			printMessage(String.format("|| Your IP and port are: <%s,%d>", ownIP.getHostAddress(), ownPort));
		} catch (Exception e) {
			e.printStackTrace();
			printMessage(e.toString());
			printMessage("ERROR: Unable to retrieve IP of localhost!");
		}
	}

	public class TimeOut implements Runnable{
		public static final int TIMELIMIT = 10;
		private DatagramPacket pkt;
		private int pktNum;

		public TimeOut(DatagramPacket pkt) {
			this.pkt = pkt;
			this.pktNum = pkt.getData()[0];
		}

		@Override
		public void run() {
			try {
				Thread.sleep(TimeUnit.SECONDS.toMillis(10));
			} catch (InterruptedException e) {
				e.printStackTrace();
				printMessage("|| ERROR: unable to wait for timeout pkt " + pktNum);
			}
			if (!acksReceived.contains(pktNum)) {
				sendPacket(pkt.getData());
			}
		}
	}


	public void sendFile(File file) {
		printMessage("||----------------");
		printMessage(String.format("|| %s starting to send file '%s'...", name, file.getName()));
		byte[] fileContent = null;
		try {
			fileContent = Files.readAllBytes(file.toPath());
			printMessage(String.format("|| %s read file to byte array, total size: %d", name, fileContent.length));
		} catch (Exception e) {
			e.printStackTrace();
			printMessage("ERROR: unable to read file to byte array");
		}

		int filePointer = 0;
		int pktNum = 0;

		printMessage(String.format("|| %s starting packet transmission", name));
		while (filePointer < fileContent.length) {
			// send x packets
			System.out.println(String.format("||------------------ %s sending next pkt", name));
			int datalen = Math.min(DATASIZE, fileContent.length - filePointer);
			byte[] pkt = new byte[HEADERSIZE + datalen];	

			pkt[0] = ((Integer) pktNum).byteValue();
			pkt[1] = ((Integer) 0).byteValue();
			pkt[2] = (filePointer + datalen >= fileContent.length) ? ((Integer) 1).byteValue() : ((Integer) 0).byteValue(); 

			System.arraycopy(fileContent, filePointer, pkt, HEADERSIZE, datalen);
			printMessage(String.format("|| %s sending packet num: %d", name, pktNum));
			sendPacket(pkt);
			pktsSent.add(pktNum);

			boolean acked = receiveAck(pktNum);
			if (acked) { 
				acksReceived.add(pktNum);
				printMessage(String.format("|| %s received ack for %d", name, pktNum));
			}
			filePointer += datalen;
			pktNum++;

			// check if all acks are in for these pkts
			// if so continue
			// else wait ff 2 x ofzo 
			// als dan nog niet gooi transmissie Exception en stop overdracht
			// ff zelf exceptie maken toch
		}
	}

	public byte[] receiveFile() {
		printMessage("||----------------");
		printMessage(String.format("|| %s waiting to receive file...", name));
		byte[] fileContent = new byte[0];

		boolean endOfFile = false;
		boolean allPktsIn = false;

		while(!endOfFile && !allPktsIn) {
			printMessage("|| Filecontent size is currently:" + fileContent.length);
			printMessage(String.format("||---------------- %s receiving next pkt", name));
			byte[] pktData = receivePacket();

			int pktNum = (int) pktData[0];
			String type = (int) pktData[1] == 0 ? "data" : "ack";
			endOfFile = (int) pktData[2] == 0 ? false : true;
			byte[] data = Arrays.copyOfRange(pktData, 3, pktData.length);

			printMessage(String.format("|| %s received %s, size:%d", name, type, data.length));

			if (endOfFile) {
				printMessage("|| This is the last packet!");
				printMessage("|| All packets in: " + allPktsIn(0, pktNum)); // TODO lastAcked until final
			}

			if (type.equalsIgnoreCase("data")) {
				if (!pktsReceived.contains(pktNum)) {
					pktsReceived.add(pktNum);

					int oldLen = fileContent.length;
					int dataLen = data.length;
					fileContent = Arrays.copyOf(fileContent,oldLen+dataLen);
					System.arraycopy(data, 0, fileContent, oldLen, dataLen);
				}
				// save and update numbers of done acks
				sendAck(pktNum);
				if (endOfFile) {
					allPktsIn = allPktsIn(0, pktNum);
					// TODO check if all are in from lastacked to final
					printMessage("|| This is the last packet!");
					printMessage("|| All packets in: " + allPktsIn);
				}
			} else if (type.equalsIgnoreCase("ack")) {
				acksReceived.add(pktNum);	
			}
			printMessage("||----------------");
		}
		printMessage("|| Final received file size is:" + fileContent.length);
		writeByte(fileContent);
		return fileContent;

		//TODO refuse packets if max storage
		// save this data
	}

	public void writeByte(byte[] fileInBytes) {		
		try { 
			setFile("bin/image1.png");  
			printMessage(String.format("|| %s writing file to system...", name));
			OutputStream os = new FileOutputStream(fileDest);  
			os.write(fileInBytes); 
			printMessage(String.format("|| %s completed writing file to system", name));
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
			printMessage("|| ERROR: unable to write file!");
		} 
	}

	public void sendPacket(byte[] data) {
		try {
			DatagramPacket send = new DatagramPacket(data, data.length, otherIP, otherPort);
			String dataType = data[1] == 1 ? "ack" : "data";
			printMessage(String.format("|| %s trying to send packet type: %s, size: %d", name, dataType, data.length));
			// als data zet timeout object en geef send mee 
			// start thread voor die timeout

			socket.send(send);
			printMessage(String.format("|| SUCCESS: %s sent packet", name));
		} catch (Exception e) {
			e.printStackTrace();
			printMessage("ERROR: sending packet not succesfull");
		}
	}

	public byte[] receivePacket() {
		try {
			byte[] buffer = new byte[HEADERSIZE + DATASIZE];
			DatagramPacket received = new DatagramPacket(buffer, buffer.length);
			socket.receive(received);	
			byte[] data = received.getData();

			if (data[1] == 1) {
				data = Arrays.copyOfRange(data, 0, HEADERSIZE);
			}
			printMessage(String.format("|| %s received packet number <%d>, size %d", name, data[0], data.length));
			//			printMessage(String.format("|| Incoming data on %s: <%s>", name, shorter(new String(received.getData())))); 
			return data;
		} catch (Exception e) {
			e.printStackTrace();
			printMessage("ERROR: unable to receive packet");
		}
		return null;
	}

	public void sendAck(int pktNum) {
		byte[] pkt = new byte[HEADERSIZE];
		pkt[0] = ((Integer) pktNum).byteValue();
		pkt[1] = ((Integer) 1).byteValue();
		pkt[2] = ((Integer) 0).byteValue();
		printMessage(String.format("|| %s acking packet <%d>", name, pktNum));
		sendPacket(pkt);
	}

	public boolean receiveAck(int pktNum) {
		byte[] receivedAck = receivePacket();
		return receivedAck[0] == pktNum && receivedAck[1] == 1;
	}

	public boolean allPktsIn(int start, int end) {
		for (int i = start; i <= end; i++) {
			if (!pktsReceived.contains(i)) {
				return false;
			}
		}
		return true;
	}

	/*
	 * ********************************************
	 * *********** Connection methods *************
	 * ******************************************** 
	 */
	public boolean createSocket() {
		int maxAttempts = 2;
		int attempts = 0;

		while (socket == null && attempts < maxAttempts) {
			try { 
				printMessage(String.format("|| Trying to set up socket for %s on <%s,%d>", name, ownIP.getHostName(), ownPort));
				socket = new DatagramSocket(ownPort);
				printMessage(String.format("|| %s socket set up!", name));
			} catch (Exception e) {
				printMessage(String.format("WARNING: Attempt %d: could not create socket on port %d", attempts, ownPort));
				attempts++;
			}
		}
		return (socket != null);
	}

	public void multicastSend() {
		try {
			byte[] msg = mcMsg1.getBytes();
			printMessage(String.format("|| %s about to send multicast...", name));
			DatagramPacket pkt = new DatagramPacket(msg, msg.length, multicastAddr, multicastPort);
			socket.send(pkt);
			printMessage(String.format("|| %s succesfully sent multicast", name));

			byte[] buffer = new byte[multicastSize];
			while (true) {
				printMessage(String.format("|| %s waiting for multicast response...", name));
				DatagramPacket response = new DatagramPacket(buffer, buffer.length);
				socket.receive(response);
				printMessage(String.format("|| %s received multicast response", name));
				printInfo(response);
				String received = new String(response.getData());
				printMessage(String.format("|| %s: multicast confirmation containing <%s>", name, received));
				if (received.equalsIgnoreCase(mcMsg2)) {
					printMessage("|| Saving info from multicast responder");
					setIPFromPkt(response);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			printMessage(String.format("|| ERROR: could not send multicast packet to %s", MULTICAST));
		}
	}

	public void multicastReceive() {
		try {
			MulticastSocket sock = new MulticastSocket(multicastPort);
			sock.joinGroup(multicastAddr);
			byte[] buffer = new byte[multicastSize];

			while(true) {
				DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
				sock.receive(pkt);
				String received = new String(pkt.getData());
				printMessage(String.format("|| %s received multicast packet containing <%s>", name, received));
				if (received.contains(mcMsg1)) { // This does not work for some reason...
					printMessage("|| Saving info from multicast sender");
					setIPFromPkt(pkt);
					sendPacket(mcMsg2.getBytes());
					break;
				}
			}
			sock.leaveGroup(multicastAddr);
			sock.close();
		} catch (IOException e) {
			printMessage(String.format("|| ERROR: %s unable to set up multicast receiver!", name));
			e.printStackTrace();
		}
	}

	public void clearConnection() {
		socket = null;
	}

	public void closeConnection() {
		printMessage(String.format("|| %s closing the connection...", name));
		try {
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setFile(String filename) {
		fileDest = new File(filename);
	}

	/*
	 * ********************************************
	 * *********** Print methods ******************
	 * ******************************************** 
	 */
	public void printInfo(DatagramPacket p) {
		printMessage(String.format("|| Packet info: ip %s, port %d", p.getAddress().toString(), p.getPort()));
	}

	public void printMessage(String s) {
		System.out.println(s);
	}

	public String shorter(String s) {
		return (s.length() > 50) ? (s.substring(0,50)+"...") : s; 
	}

	/*
	 * ********************************************
	 * *********** Getters Setters ****************
	 * ******************************************** 
	 */
	public InetAddress getOwnIP() {
		return ownIP;
	}

	public int getOwnPort() {
		return ownPort;
	}

	public void setOwnPort(int ownPort) {
		this.ownPort = ownPort;
	}

	public void setIPFromPkt(DatagramPacket p) {
		printMessage("|| Setting IP and port from received packet...");
		otherIP = p.getAddress();
		otherPort = p.getPort();
		printMessage(String.format("|| Destination IP and port are: <%s,%d>", otherIP.toString(), otherPort));
	}

	public InetAddress getOtherIP() {
		return otherIP;
	}

	public void setOtherIP(InetAddress ip) {
		this.otherIP = ip;
	}

	public int getOtherPort() {
		return otherPort;
	}

	public void setOtherPort(int port) {
		this.otherPort = port;
	}

	public String getName() {
		return name;
	}

	public DatagramSocket getSocket() {
		return this.socket;
	}
}

