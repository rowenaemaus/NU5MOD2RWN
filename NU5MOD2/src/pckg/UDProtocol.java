package pckg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


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
			printMessage(String.format("|| Your IP and port are: <%s,%d>", ownIP.getHostAddress(), ownPort));
		} catch (Exception e) {
			e.printStackTrace();
			printMessage(e.toString());
			printMessage("ERROR: Unable to retrieve IP of localhost!");
		}
	}

	public void sendFile(File file) {
		printMessage("||----------------");
		printMessage(String.format("|| %s starting to send file '%s'...", name, file.getName()));
		byte[] fileContent = null;
		try {
			fileContent = Files.readAllBytes(file.toPath());
		} catch (Exception e) {
			e.printStackTrace();
			printMessage("ERROR: unable to read file to byte array");
		}
		int fileSize = fileContent.length;

		int filePointer = 0;
		int pktNum = 0;

		while (filePointer < fileContent.length) {
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
			System.out.println("------------------");

		}
	}


	public byte[] receiveFile() {
		printMessage("||----------------");
		printMessage(String.format("|| %s starting to receive file...", name));
		byte[] fileContent = new byte[65535];

		boolean endOfFile = false;
		boolean allPktsIn = false;

		while(!endOfFile && !allPktsIn) {
			byte[] pktData = receivePacket();

			int pktNum = (int) pktData[0];
			String type = (int) pktData[1] == 0 ? "data" : "ack";
			endOfFile = (int) pktData[2] == 0 ? false : true;

			printMessage(String.format("|| %s received packet (type:%s).", name, type));

			if (endOfFile) {
				printMessage("|| This is the last packet!");
				printMessage("|| All packets in: " + allPktsIn(pktNum));
			}

			if (type.equalsIgnoreCase("data")) {
				if (!pktsReceived.contains(pktNum)) {
					pktsReceived.add(pktNum);

					int oldLen = fileContent.length;
					int dataLen = pktData.length - HEADERSIZE;
					fileContent = Arrays.copyOf(fileContent,oldLen+dataLen);
					System.arraycopy(pktData, HEADERSIZE, fileContent, oldLen, dataLen);
				}
				// save and update numbers of done acks
				sendAck(pktNum);
				if (endOfFile) {
					allPktsIn = allPktsIn(pktNum);
					printMessage("|| This is the last packet!");
					printMessage("|| All packets in: " + allPktsIn);
				}
			} else if (type.equalsIgnoreCase("ack")) {

				acksReceived.add(pktNum);	
			}
		}
		writeByte(fileContent);
		return fileContent;


		//TODO refuse packets if max storage
		// save this data
	}

	public void writeByte(byte[] fileInBytes) { 
		try { 
			setFile("src/QuotesRepro.txt");  
			printMessage(String.format("|| %s writing file to system...", name));
			OutputStream os = new FileOutputStream(fileDest);  
			os.write(fileInBytes); 
			printMessage(String.format("|| %s completed writing file to system", name));
		} catch (Exception e) {
			e.printStackTrace();
			printMessage("|| ERROR: unable to write file!");
		} 
	}

	public void sendPacket(byte[] data) {
		try {
			DatagramPacket send = new DatagramPacket(data, data.length, otherIP, otherPort);
			String dataPreview = data[1] == 1 ? "ack" : shorter(new String(data));
			printMessage(String.format("|| %s trying to send packet <%s>", name, dataPreview));
			socket.send(send);
			printMessage(String.format("|| SUCCESS: %s sent packet", name));
		} catch (Exception e) {
			e.printStackTrace();
			printMessage("ERROR: sending packet not succesfull");
		}
	}

	public byte[] receivePacket() {
		try {
			byte[] buffer = new byte[DATASIZE];
			DatagramPacket received = new DatagramPacket(buffer, buffer.length);
			socket.receive(received);		
			printMessage(String.format("|| Packet number is: <%d>", (int) received.getData()[0]));
			printMessage(String.format("|| Incoming data on %s: <%s>", name, shorter(new String(received.getData()))));

			if (otherIP == null && otherPort == -1) {
				printInfo(received);
				setIPFromPkt(received);
			}

			return received.getData();
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

	public boolean allPktsIn(int maxPktNum) {
		// TODO implement method to check if all pkts are in up until this pktNum
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

	public void getOthersIP() {
		//TODO implement broadcast etc
		setOtherIP(ownIP);
		if (name.equalsIgnoreCase("client")) {
			setOtherPort(8070);
		} else {
			setOtherPort(8071);
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

