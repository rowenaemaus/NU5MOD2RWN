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

	/**
	 * @author Rowena Emaus
	 * UDP protocol designed for Nedap University module 2 final assignment 2020
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

	private static final int HEADERSIZE = HeaderIdx.values().length; 
	private static final int DATASIZE = 256;

	Set<Integer> pktsSent = new HashSet<>();
	Set<Integer> pktsReceived = new HashSet<>();
	Set<Integer> acksReceived = new HashSet<>();

	String name;

	File fileName;
	File fileLocation;
	File[] fileList;
	String fileListString;

	public enum HeaderIdx {
		TYPE (0),
		PKTNUM (1),
		FINAL (2);

		public final int value;
		private HeaderIdx(int value) {
			this.value = value;
		}
	}

	public enum PktType {
		DATA (0, "data"), 
		ACK (1, "ack"), 
		REQUEST (2, "request"), 
		INFO (3, "filecontents"),
		GIMME (4, "pleaseTransfer"),
		DECLINE (5, "decline"),
		UNKNOWN (-1, "unknown");

		public final int value;
		public final String label;
		private PktType(int value, String label) {
			this.value = value;
			this.label = label;
		}
	}

	public enum PktFinal {
		MID (0), FINAL (1);
		public final int value;
		private PktFinal(int value) {
			this.value = value;
		}
	}

	public UDProtocol(String name, int port, File fileLocation) {
		try {
			this.ownIP = InetAddress.getLocalHost();
			this.ownPort = port;
			this.fileLocation = fileLocation;
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
			this.pktNum = pkt.getData()[HeaderIdx.PKTNUM.value];
		}

		@Override
		public void run() {
			try {
				Thread.sleep(TimeUnit.SECONDS.toMillis(10));
			} catch (InterruptedException e) {
				e.printStackTrace();
				printMessage(String.format("|| ERROR: unable to wait for timeout pkt " + pktNum));
			}
			if (!acksReceived.contains(pktNum)) {
				printMessage(String.format("|| OOPS: Timeout expired for %s's packet %d", name, pktNum));
				printMessage(String.format("|| Resending packet %d", pktNum));
				sendPacket(pkt.getData());
			}
		}
	}

	public void sendContentRequest() {
		printMessage(String.format("|| %s requests the file contents of <%s,%d>", name, otherIP.getCanonicalHostName(), otherPort));
		byte[] data = new byte[HEADERSIZE];
		data[HeaderIdx.TYPE.value] = ((Integer) PktType.REQUEST.value).byteValue();
		sendPacket(data);

		receivePacket();
	}

	public void getContentRequest() {
		printMessage(String.format("|| %s received request for content", name));
		receivePacket();
	}

	public void gimmeFile(String filename) {
		byte[] pkt = new byte[HEADERSIZE + filename.getBytes().length];
		pkt[HeaderIdx.TYPE.value] = (byte) PktType.GIMME.value;
		pkt[HeaderIdx.PKTNUM.value] = (byte) 100;
		System.arraycopy(filename.getBytes(), 0, pkt, HEADERSIZE, filename.getBytes().length);
		
		printMessage(String.format("|| %s requesting file '%s'", name, filename));
		sendPacket(pkt);

		printMessage(String.format("|| %s waiting for <%s;%d> to send file %s", name, otherIP.getCanonicalHostName(), otherPort, filename));
		
		PktType type = getPktType(receivePacket());
		if (type == PktType.ACK) {
			printMessage("|| The file is coming your way...");
		} else if (type == PktType.DECLINE) {
			printMessage("|| WARNING: File transfer of %s is not going to happen. "
					+ "The file might not exist or not be available anymore.");
		}
		receiveFile(filename);
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
		int pktNum = -1;
		acksReceived.clear();

		printMessage(String.format("|| %s starting packet transmission", name));
		while (filePointer < fileContent.length) {
			pktNum++;
			// send x packets
			System.out.println(String.format("||------------------ %s sending next pkt %d", name, pktNum));
			int datalen = Math.min(DATASIZE, fileContent.length - filePointer);
			byte[] pkt = new byte[HEADERSIZE + datalen];	

			pkt[HeaderIdx.TYPE.value] = ((Integer) PktType.DATA.value).byteValue();
			pkt[HeaderIdx.PKTNUM.value] = ((Integer) pktNum).byteValue();
			pkt[HeaderIdx.FINAL.value] = (filePointer + datalen >= fileContent.length) ? ((Integer) PktFinal.FINAL.value).byteValue() : ((Integer) PktFinal.MID.value).byteValue(); 

			System.arraycopy(fileContent, filePointer, pkt, HEADERSIZE, datalen);
			printMessage(String.format("|| %s sending packet num: %d", name, pktNum));
			// packet constructed, lets send it
			sendPacket(pkt);
			pktsSent.add(pktNum);
			receiveAck(pktNum);

			filePointer += datalen;

			// check if all acks are in for these pkts
			// if so continue
			// else wait ff 2 x ofzo 
			// als dan nog niet gooi transmissie Exception en stop overdracht
			// ff zelf exceptie maken toch
		}
	}

	public byte[] receiveFile(String filename) {
		printMessage("||----------------");
		printMessage(String.format("|| %s waiting to receive file...", name));
		byte[] fileContent = new byte[0];

		boolean endOfFile = false;
		boolean allPktsIn = false;
		pktsReceived.clear();
		
		while(!endOfFile && !allPktsIn) {
			printMessage("|| Filecontent size is currently:" + fileContent.length);
			printMessage(String.format("||---------------- %s receiving next pkt", name));
			byte[] pktData = receivePacket();

			int pktNum = (int) getPktNum(pktData);
			endOfFile = (int) pktData[HeaderIdx.FINAL.value] == PktFinal.MID.value ? false : true;
			byte[] data = Arrays.copyOfRange(pktData, HEADERSIZE, pktData.length);

			if (!pktsReceived.contains(pktNum)) { // als deze nog niet eerder gezien is, niet wegschrijven
				int oldLen = fileContent.length;
				int dataLen = data.length;
				fileContent = Arrays.copyOf(fileContent,oldLen+dataLen);
				System.arraycopy(data, 0, fileContent, oldLen, dataLen);
				pktsReceived.add(pktNum);
			}

			if (endOfFile) {
				printMessage("|| This was the last packet!");
				printMessage("|| All packets in: " + allPktsIn(0, pktNum));
				// TODO check if all are in from lastacked to final
				// what to do when not all are in but you have the final one? wait a bit?
			}
			printMessage("||----------------");
		}
		printMessage("|| Final received file size is:" + fileContent.length);
		writeByte(fileContent, filename);
		return fileContent;
	}

	public void writeByte(byte[] fileInBytes, String filename) {		
		try { 
			File file = setFile(filename);  
			printMessage(String.format("|| %s writing file to system...", name));
			OutputStream os = new FileOutputStream(file);  
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
			PktType dataType = getPktType(data[HeaderIdx.TYPE.value]);
			boolean setTimeout = dataType == PktType.DATA ? true : false;
			printMessage(String.format("|| %s trying to send packet type: %s, size: %d", name, dataType.label, data.length));
			socket.send(send);
			if (setTimeout) {new Thread(new TimeOut(send)).start();}
			printMessage(String.format("|| SUCCESS: %s sent packet type: %s", name, dataType.label));
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

			PktType pktType = getPktType(data[HeaderIdx.TYPE.value]);
			printMessage(String.format("|| %s received packet type %s", name, pktType.label));

			switch (pktType) {
			case DATA:
				handleData(data);
				return data;	
			case ACK:
				handleAck(data);
				return data;
			case REQUEST:
				handleRequest(data);
				break;
			case INFO:
				handleInfo(data);
				break;
			case GIMME:
				handleGimme(data);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			printMessage("ERROR: unable to receive packet");
		}
		return null;
	}

	public void handleData(byte[] data) {
		printPacketInfo(data);
		int pktNum = getPktNum(data);
		printMessage(String.format("|| %s sending ack for data packet num: %d", name, pktNum));
		sendAck(pktNum);
	}

	public void handleAck(byte[] data) {
		data = Arrays.copyOfRange(data, 0, HEADERSIZE);
		printPacketInfo(data);
		acksReceived.add(getPktNum(data));
	}

	public void handleRequest(byte[] data) {
		data = Arrays.copyOfRange(data, 0, HEADERSIZE);
		printPacketInfo(data);

		String contents = getFileList();
		System.out.println("SIZE OF CONTENTS IS " + contents.getBytes().length);

		byte[] pkt = new byte[HEADERSIZE+contents.getBytes().length];
		pkt[HeaderIdx.TYPE.value] = (byte) PktType.INFO.value;
		System.arraycopy(contents.getBytes(), 0, pkt, HEADERSIZE, contents.getBytes().length);

		printMessage(String.format("|| %s sent packet listing all files in '%s'", name, fileLocation.getName()));
		sendPacket(pkt);
	}

	public void handleInfo(byte[] data) {
		printPacketInfo(data);

		byte[] list = new byte[data.length-HEADERSIZE];
		System.arraycopy(data, HEADERSIZE, list, 0, data.length-HEADERSIZE);
		fileListString = new String(list);
		printMessage(String.format("|| List of contents of <%s;%d>:", otherIP.getCanonicalHostName(), otherPort));
		printMessage("--\n"+fileListString+"--");
	}

	public void handleGimme(byte[] data) {
		printPacketInfo(data);
		
		// get data excl header
		// maak file van die name
		// als die file in fiellist en in de map van filelocation
		// ack pktnum 100
		// begin sendFile van die file
	}
	
	public void sendAck(int pktNum) {
		byte[] pkt = new byte[HEADERSIZE];
		pkt[HeaderIdx.TYPE.value] = ((Integer) PktType.ACK.value).byteValue();
		pkt[HeaderIdx.PKTNUM.value] = ((Integer) pktNum).byteValue();
		pkt[HeaderIdx.FINAL.value] = ((Integer) PktFinal.MID.value).byteValue();
		printMessage(String.format("|| %s acking packet <%d>", name, pktNum));
		sendPacket(pkt);
	}

	public void receiveAck() {
		byte[] receivedAck = receivePacket();
		acksReceived.add((int) getPktNum(receivedAck)); 
	}

	public boolean receiveAck(int pktNum) {
		byte[] receivedAck = receivePacket();
		return getPktNum(receivedAck) == pktNum && receivedAck[HeaderIdx.TYPE.value] == PktType.ACK.value;
	}

	public boolean allPktsIn(int start, int end) {
		for (int i = start; i <= end; i++) {
			if (!pktsReceived.contains(i)) {
				return false;
			}
		} return true;
	}

	public String getFileList() {
		String contents = "";
		fileList = fileLocation.listFiles();

		for (File f : fileList) {
			if (f.getName().contains(".")) {
				contents += f.getName() + '\n';
			}
		}
		return contents;
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

	public File setFile(String filename) {
		return new File(fileLocation+"/"+filename+"_received");
	}

	/*
	 * ********************************************
	 * *********** Print methods ******************
	 * ******************************************** 
	 */
	public void printInfo(DatagramPacket p) {
		printMessage(String.format("|| Packet info: ip %s, port %d", p.getAddress().toString(), p.getPort()));
	}

	public void printPacketInfo(byte[] data) {
		printMessage(String.format("|| %s received packet type: %s, pktNum: %d, size: %d", name, getPktType(data[HeaderIdx.TYPE.value]).label, data[HeaderIdx.PKTNUM.value], data.length));
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

	//	public int getPktType(pktType type) {
	//		if (type == pktType.DATA) {
	//			return 0;
	//		} else if (type == pktType.ACK) {
	//			return 1;
	//		} else if (type == pktType.REQUEST) {
	//			return 2;
	//		} else {
	//			return -1;
	//		}	
	//	}
	//
	public PktType getPktType(int i) {
		if (i == PktType.DATA.value) {
			return PktType.DATA;
		} else if (i == PktType.ACK.value) {
			return PktType.ACK;
		} else if (i == PktType.REQUEST.value) {
			return PktType.REQUEST;
		} else if (i == PktType.INFO.value) {
			return PktType.INFO;
		} else if (i == PktType.GIMME.value) {
			return PktType.GIMME;
		} else if (i == PktType.DECLINE.value) {
			return PktType.DECLINE;
		}
		return PktType.UNKNOWN;
	}

	public PktType getPktType(byte[] data) {
		return getPktType(data[HeaderIdx.TYPE.value]);
	}

	public int getPktNum(byte[] data) {
		return data[HeaderIdx.PKTNUM.value];
	}
	//	
	//	public int getFinal(pktFinal finalPkt) {
	//		if (finalPkt == pktFinal.FINAL) {
	//			return 1;
	//		} else {
	//			return 0;
	//		}
	//	}
	//	
	//	public pktFinal getFinal(int i) {
	//		if (i == 1) {
	//			return pktFinal.FINAL;
	//		} else {
	//			return pktFinal.MID;
	//		}
	//	}

}
