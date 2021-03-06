package udp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

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

	// Variables for the multicast functionality
	private static final String MULTICAST = "230.0.0.0";
	private InetAddress multicastAddr;
	private int multicastPort = 5555;
	private int multicastSize;
	private String mcMsg1 = "hiserver";
	private String mcMsg2 = "hiclient";

	// Packet related constants
	public static final int HEADERSIZE = HeaderIdx.values().length; 
	public static final int DATASIZE = 256;
	public static final int MAXPKTNUM = 127;

	// Sets to keep track of sent and received packets
	Set<Integer> pktsSent = new HashSet<>();
	Set<Integer> pktsReceived = new HashSet<>();
	Set<Integer> acksReceived = new HashSet<>();

	// Variables related to files on server
	private File fileLocation;
	private File[] fileList;
	private Set<String> availableFiles = new HashSet<String>();
	private String fileListString;
	private BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
	private static final int MAXFILES = 15;

	private Set<String> allowedExtension = new HashSet<String>(Arrays.asList("txt", "png", "pdf"));

	/** 
	 * HeadIdx - the index of the header where certain types of information 
	 * are stored. 
	 * 
	 */
	public enum HeaderIdx {
		TYPE (0),
		PKTNUM (1),
		FINAL (2);

		public final int value;
		private HeaderIdx(int value) {
			this.value = value;
		}
	}

	/**
	 * PktType - Different types of packets that can be sent
	 * and received by either server or client. Each type of 
	 * packet has a designated PacketHandler to further process 
	 * the packet.
	 *
	 */
	public enum PktType {
		DATA (0, "data", new HandleData()), 
		ACK (1, "ack", new HandleAck()), 
		REQUEST (2, "request", new HandleRequest()), 
		INFO (3, "filecontents", new HandleInfo()),
		GIMME (4, "pleaseTransfer", new HandleGimme()),
		DECLINE (5, "decline", new HandleDecline()),
		DELETE (6, "delete", new HandleDelete()),
		FILE (7, "file", new HandleFile()),
		UNKNOWN (-1, "unknown", new HandleUnknown());

		public final int value;
		public final String label;
		public PacketHandler handler;
		private PktType(int value, String label, PacketHandler handler) {
			this.value = value;
			this.label = label;
			this.handler = handler;
		}

		public static PktType getType(int i) {
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

		public PacketHandler getHandler() {
			return handler;
		}
	}

	/**
	 * PktFinal - Indicates whether a data packet is the last 
	 * packet of to be received data or not. This way the final 
	 * packet - ack combination will not be lost.
	 *
	 */
	public enum PktFinal {
		MID (0), FINAL (1);
		public final int value;
		private PktFinal(int value) {
			this.value = value;
		}
	}

	/**
	 * Constructor of UDP instance
	 * @param name name of the user of this protocol
	 * @param port preferred port to open datagramsocket on
	 * @param fileLocation location where files are stored (for upload and download)
	 */
	public UDProtocol(String name, int port, File fileLocation) {
		try {
			this.name = name;
			this.ownIP = getNetworkIP();
			this.ownPort = port;
			this.fileLocation = fileLocation;

			this.multicastAddr = InetAddress.getByName(MULTICAST);
			this.multicastSize = mcMsg1.getBytes().length;
			printMessage(String.format("|| Your IP and port are: <%s,%d>", ownIP.getHostAddress(), ownPort));
		} catch (Exception e) {
			e.printStackTrace();
			printMessage(e.toString());
			printMessage("ERROR: Unable to retrieve IP of localhost!");
		}
	}

	/**
	 * A class to keep track of the timeout since
	 * a packet was sent. An instance of this object is 
	 * created when a PktType.DATA packet is sent. As soon
	 * as the timer runs out, it checks whether the ack came 
	 * in, and if not resends the data (creating new timeout
	 * instance, this one dies)
	 */
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

	/**
	 * Requests a list of the available files to the receiver of
	 * this request.
	 */
	public void sendContentRequest() {
		printMessage(String.format("|| %s requests the file contents of <%s,%d>", name, otherIP.getCanonicalHostName(), otherPort));
		byte[] data = new byte[HEADERSIZE];
		data[HeaderIdx.TYPE.value] = ((Integer) PktType.REQUEST.value).byteValue();
		sendPacket(data);

		receivePacket();
	}

	/**
	 * Listens for a contentRequest of all available files. The
	 * handling of the request is done in a PacketHandler 
	 * implementation
	 */
	public void getContentRequest() {
		receivePacket();
		printMessage(String.format("|| %s received request for content", name));
	}

	/**
	 * Sends a request for file transfer to the receiver of this
	 * request. After sending the request for files, first a packet
	 * receives, confirming or declining the transfer of the file.
	 * @param filename name of the requested file
	 */
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

	/**
	 * Sends a request to delete a file. The sender of this request 
	 * receives a confirmation/decline before the deletion. And after 
	 * the deletion of the file receives a confirmation/decline of
	 * actual success of the file removal.
	 * @param filename name of the file to be deleted
	 */
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

	/**
	 * Method to remove a file. File is removed from fileList and
	 * availableFiles to remove all references.
	 * @param f file to be deleted
	 * @return whether file was successfully deleted
	 */
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

	/**
	 * Sends a file through datagrampackets. The file is read to byte
	 * array, and then cut up in datagrampackets of size DATASIZE.
	 * The header carries the packet number of the data packet. After 
	 * 128 packets, the packetnumber is reset to 0 (pktNum is 1 byte).
	 * After sending all the data, statistics of the transmission are
	 * printed, and the hash of the original datafile is sent.
	 * The timeout is set in sendPacket().
	 * @param file
	 */
	public void sendFile(File file) {
		Statistics stats = new Statistics(file.getName());
		printMessage("||----------------");
		printMessage(String.format("|| %s starting to send file '%s'...", name, file.getName()));
		byte[] fileContent = null;
		try {
			fileContent = Files.readAllBytes(file.toPath());
			stats.setTotalFileSize(fileContent.length);
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
		stats.setStartTransmission(System.currentTimeMillis());
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
			stats.addSent();
			pktsSent.add(pktNum);
			receiveAck(pktNum);
			stats.addReceived();

			filePointer += datalen;

			if (pktNum == MAXPKTNUM) {
				pktNum = -1;
				pktsSent.clear();
			}
		}
		stats.setEndTransmission(System.currentTimeMillis());
		stats.printStats();

		sendHashFile(fileContent);	
		printMessage(String.format("|| %s sent hash of file: '%s'", name, file.getName()));
	}

	/**
	 * Receives a file packet in datagrampackets. The packets are acked
	 * by pktNum of the header. The listening for data packets stops as
	 * soon as the header carries the PktFinal.FINAL value.
	 * Every 5 packets, it is checked whether during transmission there
	 * was a p typed in the console. If this is the case, the download
	 * is paused. 
	 * After the final packet is in, statistics on the transmission 
	 * are displayed and the hash is received. When the hash matches 
	 * the received data, the file is stored. Otherwise it is discarded.
	 * @param filename name of the file to be saved
	 * @return contents of the file that is received
	 */
	public byte[] receiveFile(String filename) {
		Statistics stats = new Statistics(filename);
		printMessage("||----------------");
		printMessage(String.format("|| %s waiting to receive file...", name));
		byte[] fileContent = new byte[0];

		boolean endOfFile = false;
		boolean allPktsIn = false;
		pktsReceived.clear();

		stats.setStartTransmission(System.currentTimeMillis());
		while(!endOfFile && !allPktsIn) {
			printMessage("|| Filecontent size is currently:" + fileContent.length);
			printMessage(String.format("||---------------- %s receiving next pkt", name));
			byte[] pktData = receivePacket();
			stats.addReceived();

			int pktNum = (int) getPktNum(pktData);
			endOfFile = (int) pktData[HeaderIdx.FINAL.value] == PktFinal.MID.value ? false : true;
			byte[] data = Arrays.copyOfRange(pktData, HEADERSIZE, pktData.length);

			if (!pktsReceived.contains(pktNum)) {
				int oldLen = fileContent.length;
				int dataLen = data.length;
				fileContent = Arrays.copyOf(fileContent,oldLen+dataLen);
				System.arraycopy(data, 0, fileContent, oldLen, dataLen);
				pktsReceived.add(pktNum);
			} else {
				stats.addRetransmit();
			}

			if (endOfFile) {
				stats.setEndTransmission(System.currentTimeMillis());
				printMessage("|| This was the last packet!");
				printMessage("|| All packets in: " + allPktsIn(0, pktNum));
			}
			if (pktNum == MAXPKTNUM) {
				pktsReceived.clear();
			}

			if (Math.floorMod(pktNum, 5) == 0) {
				checkPause();
			}
			printMessage("||----------------");
		}
		printMessage("|| Final received file size is:" + fileContent.length);
		stats.setTotalFileSize(fileContent.length);
		stats.printStats();

		String hashReceived = new String(receiveHash());
		String hashComputed = new String(getHash(fileContent));
		printMessage(String.format("|| %s received hash of original file '%s': %s", name, filename, hashReceived));
		printMessage(String.format("|| %s computed hash of received file '%s': %s", name, filename, hashComputed));

		if (hashReceived.contentEquals(hashComputed)) {
			printMessage(String.format("|| SUCCESS: Integrity of file '%s' checks. %s writing file to system...", filename, name));
			writeByte(fileContent, filename);
		} else {
			printMessage(String.format("|| WARNING: Integrity of file '%s' does not check. %s not writing file to system.", filename, name));
		}
		return fileContent;
	}

	/**
	 * Checks whether 'p' + enter is in the BufferedReader
	 * keyboard. If so, it wil pause the transmission until
	 * another key + enter is hit.
	 */
	public void checkPause() {
		String pause = "p";
		String input = "";

		try {
			if (keyboard.ready()) {
				input = keyboard.readLine(); 
				if (input.equalsIgnoreCase(pause)) {
					printMessage("................");
					printMessage("................");
					printMessage("PAUSING DOWNLOAD");
					printMessage("Hit any key + enter to resume");
					printMessage("................");
					printMessage("................");
					input = keyboard.readLine();
				}
			} else {
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

	/**
	 * Writes the received byte[] to a file on the system using
	 * OutputStream. 
	 * @param fileInBytes the byte[] of the received data
	 * @param filename name of the file to be saved
	 */
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

	/**
	 * Sends a datagrampacket containing header and data provided in 
	 * the argument as body. After building the packet sends it over
	 * the datagramsocket.
	 * @param data
	 */
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
			printMessage(e.getMessage());
			printMessage("ERROR: sending packet not succesfull");
		}
	}

	/**
	 * Receives data from the datagramsocket. After reception, the 
	 * packet type is extracted and further handling is done by specified 
	 * PacketHandlers defined in the PktType enum. 
	 * @return data that is received (including header)
	 */
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

	/**
	 * Sends an ack type packet. Given the packetnumber constructs a packet
	 * of type PktType.ACK and sets the pktnum in the header. 
	 * @param pktNum
	 */
	public void sendAck(int pktNum) {
		byte[] pkt = new byte[HEADERSIZE];
		pkt[HeaderIdx.TYPE.value] = ((Integer) PktType.ACK.value).byteValue();
		pkt[HeaderIdx.PKTNUM.value] = ((Integer) pktNum).byteValue();
		printMessage(String.format("|| %s acking packet <%d>", name, pktNum));
		sendPacket(pkt);
	}

	/**
	 * Receives acks. Gets the data from the receivePacket() and adds the
	 * received number to acksReceived. This method does nothing special 
	 * on top of HandleAck. 
	 * >>>> Nominated for deletion. 
	 */
	public void receiveAck() {
		byte[] receivedAck = receivePacket();
		acksReceived.add((int) getPktNum(receivedAck)); 
	}

	/**
	 * Receives acks. Listens for an ack of a specific pktNum
	 * @param pktNum the pktNum to check for acks
	 * @return whether this pktNum has arrived. 
	 * >>>> Nominated for deletion
	 */
	public boolean receiveAck(int pktNum) {
		byte[] receivedAck = receivePacket();
		return getPktNum(receivedAck) == pktNum && receivedAck[HeaderIdx.TYPE.value] == PktType.ACK.value;
	}

	/**
	 * Sends decline type datagrampackets
	 */
	public void sendDecline() {
		byte[] pkt = new byte[HEADERSIZE];
		pkt[HeaderIdx.TYPE.value] = ((Integer) PktType.DECLINE.value).byteValue();
		printMessage(String.format("|| %s declining request", name));
		sendPacket(pkt);
	}

	/**
	 * Sends datagrampacket with hash of provided byte[] argument
	 * @param data data to be converted to hash
	 */
	public void sendHashFile(byte[] data) {
		byte[] hash = getHash(data);
		byte[] pkt = new byte[HEADERSIZE + hash.length];

		pkt[HeaderIdx.TYPE.value] = ((Integer) PktType.ACK.value).byteValue();
		pkt[HeaderIdx.PKTNUM.value] = (byte) hash.length;
		System.arraycopy(hash, 0, pkt, HEADERSIZE, hash.length);

		sendPacket(pkt);
	}

	/**
	 * Receives datagrampacket with hash of original data
	 * @return hash code of original data, extracted from packet
	 */
	public byte[] receiveHash() {
		byte[] pkt = receivePacket();
		int size = getPktNum(pkt);
		byte[] hash = new byte[size];
		System.arraycopy(pkt, HEADERSIZE, hash, 0, size);
		return hash;
	}

	/**
	 * Computes the hash of provided byte[] argument.
	 * @param data to be converted data
	 * @return hash byte[] of data
	 */
	public byte[] getHash(byte[] data) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(data);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return md.digest();
	}

	/**
	 * Checks if all packets from index start to index end
	 * are in.
	 * @param start index from where to check
	 * @param end index to where to check
	 * @return Whether all packet nums between start and end are in
	 */
	public boolean allPktsIn(int start, int end) {
		for (int i = start; i <= end; i++) {
			if (!pktsReceived.contains(i)) {
				return false;
			}
		} return true;
	}

	/*
	 * Forms 1 long String of all the Strings in AvailableFiles 
	 */
	public String getContentlistString() {
		updateContentList();
		String contents = "";
		for (String file : availableFiles) {
			contents += file + '\n';
		}
		return contents;
	}

	/**
	 * Updates the current list of available files. Only
	 * list those that are of the permitted type.
	 */
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
	
	/**
	 * Extracts all network interfaces of current device.
	 * Scans for ip-addresses and selects the one starting
	 * with 'compare' to select local network ip.
	 * @return local IP starting with 'compare'
	 */
	public InetAddress getNetworkIP() {
		String compare = "192.168.";
		InetAddress localIP = null;

		Enumeration<NetworkInterface> nets = null;
		printMessage(String.format("|| %s trying to extracting network interfaces", name));
		try {
			nets = NetworkInterface.getNetworkInterfaces();		
			printMessage(String.format("|| %s done extracting network interfaces", name));
		} catch (Exception e) {
			e.printStackTrace();
			printMessage(String.format("|| ERROR: %s unable to fetch network interfaces", name));		
		}

		printMessage(String.format("|| %s trying to find local ip of own machine", name));
		while (nets.hasMoreElements()) {
			NetworkInterface nif = nets.nextElement();
			Enumeration<InetAddress> a = nif.getInetAddresses();
			while (a.hasMoreElements()) {
				InetAddress addr = a.nextElement();
				if (addr.getHostAddress().substring(0, compare.length()).equalsIgnoreCase(compare)) {
					localIP = addr;
					printMessage(String.format("|| %s setting own IP to: <%s>", name, localIP.getHostAddress()));
				}
			}	
		}
		return localIP;
	}

	/**
	 * Sets up a datagramSocket
	 * @return whether socket was set up successfully
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

	/**
	 * Sends a pre-defined multicast message over the datagramsocket.
	 * Then listens for a predefined response String. If this 
	 * String is received, the IP and port of the sender are stored.
	 */
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

	/**
	 * Sets up a multicast socket and joins a predefined group address.
	 * Then listens for a message to come in on this socket. If this 
	 * message is the predefined connect message, a predefined response 
	 * is sent back to sender. The IP and port of this sender is saved.
	 * Afterwards, the multicast group is left and socket closed.  
	 */
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

	/**
	 * Creates a File object provided filename and globally defined
	 * path/location
	 * @param filename
	 * @return
	 */
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

	/**
	 * Prints info of a packet
	 * @param data
	 */
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

	/**
	 * Checks if a file with a certain filename is already
	 * in this folder, otherwise adds '_new' to it.
	 * @param s
	 * @return
	 */
	public String checkFilename(String s) {
		updateContentList();
		if (availableFiles.contains(s)) {
			s = fileNewVersion(s);
		}		
		return s;
	}

	/**
	 * Adds '_new' to the provided s.
	 * @param s
	 * @return
	 */
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
	
	public int getMaxFiles() {
		return UDProtocol.MAXFILES;
	}

	public long getCRC(byte[] data) {
		CRC32 crc = new CRC32();
		crc.update(data, 0, data.length);
		printMessage(String.format("|| %s computing crc for data", name));
		return crc.getValue();
	}

}
