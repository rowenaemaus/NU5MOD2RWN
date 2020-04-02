package Pckg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;


public class UDPServer implements Runnable{

	private DatagramSocket ssock;
	private int port;
	private static String serverName = "~~ ServeRowena ~~";
	private String IPAddress;
	private Scanner inputScanner;
	private PrintStream outputStream;

	@Override
	public void run() {
		boolean openNewSocket = true;
		while (openNewSocket) {
			try {
				setupSsock();
				printMessage("]] Server socket set up");

				//				inputScanner = new Scanner(new BufferedInputStream(sock.getInputStream()));
				//				outputStream = new PrintStream(new BufferedOutputStream(sock.getOutputStream()));
				//				printMessage("> Server I/O set up.");

				while(true) {
					// wait for client request
					String filename = ("src/image1.png");
					
					// read in file
					// read file in
					Integer[] fileContents = readFile(filename);
					printMessage("]] Server file read");
					
					// make into packets

					// send using stop wait
				}

				//		try {
				//			ServerSocket serverSocket = new ServerSocket(port);
				//			Socket connectionSocket = serverSocket.accept();
				//
				//			inputScanner = new Scanner(new BufferedInputStream(
				//					connectionSocket.getInputStream()));
				//			outputStream = new PrintStream(new BufferedOutputStream(
				//					connectionSocket.getOutputStream()));
				//
				//
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public Integer[] readFile(String filename) {
		try {
			File fileToSend = new File(filename);
			FileInputStream fileStream = new FileInputStream(fileToSend);
			Integer[] filecontent = new Integer[(int) fileToSend.length()];

			for (int i = 0; i < filecontent.length; i++) {
				int nextByte = fileStream.read();
				if (nextByte == -1) {
					throw new Exception("ERROR: File size is smaller than reported");
				}
				filecontent[i] = nextByte;
			}
			return filecontent;
		} catch (Exception e) {
			e.printStackTrace();
			printMessage("ERROR: Server unable to setup filestream");
			return null;
		}
	}

	public void setupSsock() {
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
				printMessage(String.format("Trying to open a socket at %s on port %d", IPAddress, port));
				//				ssock = new ServerSocket(port, 0, InetAddress.getByName(IPAddress));
				ssock = new DatagramSocket(port, InetAddress.getByName(IPAddress));
				printMessage("]] Server started at port " + port);
				printMessage("----------------------------------------------");
				printMessage("----------------------------------------------");
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
