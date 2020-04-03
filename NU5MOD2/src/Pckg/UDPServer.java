package Pckg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;


public class UDPServer implements Runnable{

	private DatagramSocket ssock;
	private int port;
	private static String serverName = "~~ ServeRowena ~~";
	private String IPAddress;
	private Scanner inputScanner;
	private PrintStream outputStream;

	private InetAddress clientAddress;
	private int clientPort;

	boolean clientConnected = false;

	int maxFiles = 10;
	int numFiles;
	File fileLocation = new File("src/");

	@Override
	public void run() {
		boolean openNewSocket = true;
		while (openNewSocket) {
			try {
				setupSock();
				printMessage("]] Server socket set up");

				numFiles = countFiles();
				printMessage(String.format("]] Server stores %d files", numFiles));
				printMemory();

				//				inputScanner = new Scanner(new BufferedInputStream(sock.getInputStream()));
				//				outputStream = new PrintStream(new BufferedOutputStream(sock.getOutputStream()));
				//				printMessage("> Server I/O set up.");

				printMessage("]] Server waiting to connect with client");
				connectionRequest();

				while(true) {
					printMessage("]] Server waiting for incoming packets");
					receivePacket();




					// wait for client request of image
					String filename = ("src/image1.png");

					// read file in
					byte[] fileContents = readFile(filename);
					printMessage("]] Server file read");

					// make into packets

					// send using stop wait
					printMessage("]] Getting ready to send");
					sendPacket(fileContents);	
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void sendPacket(byte[] data) {
		try {
			DatagramPacket response = new DatagramPacket(data, data.length, clientAddress, clientPort);
			ssock.send(response);
			printMessage("]] Server packet sent");
		} catch (Exception e) {
			e.printStackTrace();
			printMessage("ERROR: sending packet not succesfull");
		}
	}

	private void receivePacket() {
		try {
			DatagramPacket request = new DatagramPacket(new byte[65535], 65535);
			ssock.receive(request);
			printInfo(request);
			
			
			String data = new String(request.getData());
			printMessage("]] Incoming data: " + data);
			
			// TODO extract data
			// save this data
			// if memory full block it
			
		} catch (Exception e) {
			e.printStackTrace();
			printMessage("ERROR: unable to receive packet");
		}
		//TODO refuse packets if max storage
	}

	public void checkMemory(byte[] data) {
		// TODO check if max files is reached. Otherwise refuse to save.	
	}
	
	public void connectionRequest() {
		try {
			DatagramPacket request = new DatagramPacket(new byte[1], 1);
			ssock.receive(request);
			clientAddress = request.getAddress();
			clientPort = request.getPort();
			printInfo(request);
			printMessage("]] Connection packet received");
			printMessage("]] Server sending ack");
			sendPacket("Connect".getBytes());
		} catch (IOException e) {
			printMessage("ERROR: failed connection request!");
			e.printStackTrace();
		}
	}

	public void printInfo(DatagramPacket p) {
		printMessage(String.format("]] Packet info: ip %s, port %d", p.getAddress().toString(), p.getPort()));
	}

	public byte[] readFile(String filename) {
		try {
			printMessage(String.format("]] Ready to read file %s", filename));
			File fileToSend = new File(filename);
			byte[] fileContent = Files.readAllBytes(fileToSend.toPath());
			printMessage(String.format("]] File <%s> read", filename));

			//			FileInputStream fileStream = new FileInputStream(fileToSend);
			//			Integer[] filecontent = new Integer[(int) fileToSend.length()];
			//
			//			for (int i = 0; i < filecontent.length; i++) {
			//				int nextByte = fileStream.read();
			//				if (nextByte == -1) {
			//					throw new Exception("ERROR: File size is smaller than reported");
			//				}
			//				filecontent[i] = nextByte;
			//			}
			return fileContent;
		} catch (Exception e) {
			e.printStackTrace();
			printMessage("ERROR: Server unable to setup filestream");
			return null;
		}
	}

	public int countFiles() {
		File folder = new File("src/");
		return folder.listFiles().length;
	}

	public void printMemory() {
		printMessage("]] Memory contains:");
		File[] files = fileLocation.listFiles();

		int i = 0;
		for (File file : files){
			if (file.isFile()) {
				i++;
				printMessage(String.format("]] %(-10d: %s", i, file.getName()));
			}
		}
		printMessage("]] ------");
		if (files.length < maxFiles) {
			printMessage(String.format("]] %-10s: Add new file", "O"));
		} else {
			printMessage("]] -- Adding files not possible, max storage used --");
		}
		printMessage(String.format("]] %-10s: Remove file", "X"));
		printMessage("\n");
	}

	public void setupSock() {
		try {
			IPAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			printMessage("ERROR: Unable to retrieve IP address of localhost!");
			e.printStackTrace();
		}

		ssock = null;
		int maxAttempts = 2;
		int attempts = 0;
		while (ssock == null && attempts < maxAttempts) {
			int port = getPort();
			try {
				printMessage(String.format("]] Trying to open a socket at %s on port %d", IPAddress, port));
				//				ssock = new ServerSocket(port, 0, InetAddress.getByName(IPAddress));
				ssock = new DatagramSocket(port);
				printMessage("]] Server started at port " + port);
				printMessage("]] ----------------------------------------------");
				printMessage("]] ----------------------------------------------");
			} catch (IOException e) {
				printMessage(String.format("WARNING: Attempt %d: could not create socket on port %d", attempts, port));
				attempts++;
			}
		}

	}
	

	public int getPort() {
		//TODO implement
		return 8070;
	}
	

	public void printMessage(String s) {
		System.out.println(s);
	}	

	public static void main (String[] args) {
		UDPServer srv = new UDPServer();
		System.out.println(String.format("]] Welcome to %s, ready to host...", serverName));
		try {
			System.out.println("]] My IP address is " + InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			System.out.println("ERROR: Unable to retrieve IP");
			e.printStackTrace();
		}
		new Thread(srv).start();
	}
}
