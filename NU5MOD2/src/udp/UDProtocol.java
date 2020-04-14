package udp;

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

	public String name;
	private DatagramSocket socket = null;
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

	public static final int HEADERSIZE = HeaderIdx.values().length; 
	public static final int DATASIZE = 256;
	public static final int MAXPKTNUM = 127;
	public static final int MAXFILESIZE = 10000000;

	Set<Integer> pktsSent = new HashSet<>();
	Set<Integer> pktsReceived = new HashSet<>();
	Set<Integer> acksReceived = new HashSet<>();

	private File fileLocation;
	private File[] fileList;
	private Set<String> availableFiles = new HashSet<String>();
	private String fileListString;

	private Set<String> allowedExtension = new HashSet<String>(Arrays.asList("txt", "png", "pdf"));

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
		DATA (0, "data", new HandleData()), 
		ACK (1, "ack", new HandleAck()), 
		REQUEST (2, "request", new HandleRequest()), 
		INFO (3, "filecontents", new HandleInfo()),
		GIMME (4, "pleaseTransfer", new HandleGimme()),
		DECLINE (5, "decline", new HandleDecline()),
		DELETE (6, "delete", new HandleDelete()),
		FILE (7, "file", new HandleFile()),
		UNKNOWN (-1, "unknown", null);

		public final int value;
		public final String label;
		public PacketHandler handler;
		private PktType(int value, String label, PacketHandler handler) {
			this.value = value;
			this.label = label;
			this.handler = handler;
		}

		static PktType getType(int i) {
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
			} else if (i == PktType.DELETE.value) {
				return PktType.DELETE;
			} else if (i == PktType.FILE.value) {
				return PktType.FILE;
			}
			return PktType.UNKNOWN;
		}

		PacketHandler getHandler() {
			return handler;
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
		public static final int TIMELIMIT = 3;
		private DatagramPacket pkt;
		private int pktNum;

		public TimeOut(DatagramPacket pkt) {
			this.pkt = pkt;
			this.pktNum = pkt.getData()[HeaderIdx.PKTNUM.value];
		}

		@Override
		public void run() {
			try {
				Thread.sleep(TimeUnit.SECONDS.toMillis(TIMELIMIT));
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
		receivePacket();
		printMessage(String.format("|| %s received request for content", name));
	}

	public void gimmeFile(String filename) {
		byte[] pkt = new byte[HEADERSIZE + filename.getBytes().length];
		pkt[HeaderIdx.TYPE.value] = (byte) PktType.GIMME.value;
		pkt[HeaderIdx.PKTNUM.value] = (byte) filename.getBytes().length;
		System.arraycopy(filename.getBytes(), 0, pkt, HEADERSIZE, filename.getBytes().length);

		printMessage(String.format("|| %s requesting file '%s'", name, filename));
		sendPacket(pkt);

		printMessage(String.format("|| %s waiting for <%s;%d> to send file %s", name, otherIP.getCanonicalHostName(), otherPort, filename));

		PktType type = getPktType(receivePacket());
		if (type == PktType.ACK) {
			printMessage("|| The file is coming your way...");
			receiveFile(filename);
		} else if (type == PktType.DECLINE) {
			printMessage(String.format("|| WARNING: File transfer of %s is not going to happen. "
					+ "The file might not exist or not be available anymore.", filename));
		}
	}

	public void deleteFile(String filename) {
		byte[] pkt = new byte[HEADERSIZE + filename.getBytes().length];
		pkt[HeaderIdx.TYPE.value] = (byte) PktType.DELETE.value;
		pkt[HeaderIdx.PKTNUM.value] = (byte) filename.getBytes().length;
		System.arraycopy(filename.getBytes(), 0, pkt, HEADERSIZE, filename.getBytes().length);

		printMessage(String.format("|| %s requesting deleting of file '%s'", name, filename));
		sendPacket(pkt);

		printMessage(String.format("|| %s waiting for <%s;%d> to delete file %s", name, otherIP.getCanonicalHostName(), otherPort, filename));

		PktType type = getPktType(receivePacket());
		if (type == PktType.ACK) {
			printMessage("|| The file is about to be deleted...");
		} else if (type == PktType.DECLINE) {
			printMessage(String.format("|| WARNING: File deletion of %s is not going to happen. "
					+ "The file might not exist or not be available anymore.", filename));
		}
		// deleting is attempted here, ack if success
		type = getPktType(receivePacket());
		if (type == PktType.ACK) {
			printMessage("|| The file is succesfully deleted!");
		} else if (type == PktType.DECLINE) {
			printMessage(String.format("|| WARNING: File deletion of %s did not happen. "
					+ "The file might not exist or not be available anymore.", filename));
		}		
	}

	public boolean deleteFromServer(File f) {
		if(f.delete()) {
			printMessage(String.format("|| SUCCESS: %s deleted %s", name, f.getName()));
			fileList = null;
			availableFiles.clear();
			return true;
		} else {
			printMessage(String.format("|| FAILURE: %s failed to delete %s", name, f.getName()));
			return false;
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
			return;
		}

		int filePointer = 0;
		int pktNum = -1;
		acksReceived.clear();

		printMessage(String.format("|| %s starting packet transmission", name));
		while (filePointer < fileContent.length) {
			pktNum++;	
			printMessage(String.format("||------------------ %s sending next pkt %d", name, pktNum));
			int datalen = Math.min(DATASIZE, fileContent.length - filePointer);
			byte[] pkt = new byte[HEADERSIZE + datalen];	

			pkt[HeaderIdx.TYPE.value] = ((Integer) PktType.DATA.value).byteValue();
			pkt[HeaderIdx.PKTNUM.value] = ((Integer) pktNum).byteValue();
			pkt[HeaderIdx.FINAL.value] = (filePointer + datalen >= fileContent.length) ? ((Integer) PktFinal.FINAL.value).byteValue() : ((Integer) PktFinal.MID.value).byteValue(); 

			System.arraycopy(fileContent, filePointer, pkt, HEADERSIZE, datalen);
			printMessage(String.format("|| %s sending packet num: %d", name, pktNum));
			sendPacket(pkt);
			pktsSent.add(pktNum);
			receiveAck(pktNum);

			filePointer += datalen;

			if (pktNum == MAXPKTNUM) {
				pktNum = -1;
				pktsSent.clear();

			}

			// check if all acks are in for these pkts
			// if so continue
			// else wait ff 2 x ofzo 
			// als dan nog niet gooi transmissie Exception en stop overdracht
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
			if (pktNum == MAXPKTNUM) {
				pktsReceived.clear();
			}
			printMessage("||----------------");
		}
		printMessage("|| Final received file size is:" + fileContent.length);
		writeByte(fileContent, filename);
		return fileContent;
	}

	public void writeByte(byte[] fileInBytes, String filename) {		
		try { 
			File file = setFile(checkFilename(filename));
			printMessage(String.format("|| %s stored file at :'%s", name, file.getAbsolutePath()));
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
			PktType dataType = PktType.getType((data[HeaderIdx.TYPE.value]));
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

			PktType pktType = PktType.getType((data[HeaderIdx.TYPE.value]));
			printMessage(String.format("|| %s received packet type %s", name, pktType.label));

			pktType.getHandler().handlePkt(this, data);
			return data;

		} catch (Exception e) {
			e.printStackTrace();
			printMessage("ERROR: unable to receive packet");
			return null;
		}
	}


	public void sendAck(int pktNum) {
		byte[] pkt = new byte[HEADERSIZE];
		pkt[HeaderIdx.TYPE.value] = ((Integer) PktType.ACK.value).byteValue();
		pkt[HeaderIdx.PKTNUM.value] = ((Integer) pktNum).byteValue();
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

	public void sendDecline() {
		byte[] pkt = new byte[HEADERSIZE];
		pkt[HeaderIdx.TYPE.value] = ((Integer) PktType.DECLINE.value).byteValue();
		printMessage(String.format("|| %s declining request", name));
		sendPacket(pkt);
	}

	public boolean allPktsIn(int start, int end) {
		for (int i = start; i <= end; i++) {
			if (!pktsReceived.contains(i)) {
				return false;
			}
		} return true;
	}


	public String getContentlistString() {
		updateContentList();
		String contents = "";
		for (String file : availableFiles) {
			contents += file + '\n';
		}
		return contents;
	}

	public void updateContentList() {
		fileList = getFileLocation().listFiles();
		for (File f : getFileList()) {
			String extension = getExtension(f.getName());
			if (allowedExtension.contains(extension)) {
				availableFiles.add(f.getName());
			}
		}
	}

	/*
	 * ********************************************
	 * *********** Connection methods *************
	 * ******************************************** 
	 */
	public boolean createSocket() {
		int maxAttempts = 2;
		int attempts = 1;

		while (socket == null && attempts <= maxAttempts) {
			try { 
				printMessage(String.format("|| Trying to set up socket for %s on <%s,%d>", name, ownIP.getHostName(), ownPort));
				socket = new DatagramSocket(ownPort, ownIP);
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
			printMessage(String.format("|| %s multicast socket initialised", name));
			sock.joinGroup(multicastAddr);
			printMessage(String.format("|| %s joined multicast socket", name));
			byte[] buffer = new byte[multicastSize];

			while(true) {
				DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
				printMessage("|| Getting ready to receive first pkt on multicast");
				sock.receive(pkt);
				printMessage(String.format("|| %s received pkt on multicast", name));
				String received = new String(pkt.getData());
				printMessage(String.format("|| %s received multicast packet containing <%s>", name, received));
				if (received.contains(mcMsg1)) {
					printMessage("|| Saving info from multicast sender");
					setIPFromPkt(pkt);
					printMessage(String.format("|| %s setting up datagramsocket...", name));
					createSocket();
					printMessage(String.format("|| %s socket set up. Sending confirmation to client...", name));
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
		closeConnection();
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
		return new File(getFileLocation().toString()+"/"+filename);
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
		printMessage(String.format("|| %s received packet type: %s, pktNum: %d, size: %d", name, PktType.getType((data[HeaderIdx.TYPE.value])).label, data[HeaderIdx.PKTNUM.value], data.length));
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


	public PktType getPktType(byte[] data) {
		return PktType.getType((data[HeaderIdx.TYPE.value]));
	}


	public int getPktNum(byte[] data) {
		return data[HeaderIdx.PKTNUM.value];
	}


	public String getExtension(String s) {
		int i = s.indexOf(".");
		return i == -1 || i == 0 ? "" : s.substring(i+1);
	}

	public String checkFilename(String s) {
		updateContentList();
		if (availableFiles.contains(s)) {
			s = fileNewVersion(s);
		}		
		return s;
	}

	public String fileNewVersion(String s) {
		String ext = getExtension(s);
		int i = s.indexOf(".");
		s = s.substring(0, i);
		s = s + "_new." + ext;
		return s;	
	}

	public String getFileListString() {
		return fileListString;
	}

	public Set<String> getAvailableFiles() {
		return availableFiles;
	}

	public void setAvailableFiles(Set<String> availableFiles) {
		this.availableFiles = availableFiles;
	}

	public File getFileLocation() {
		return fileLocation;
	}

	public File[] getFileList() {
		return fileList;
	}

	public void setFileListString(String string) {
		this.fileListString = string;  

	}
}
