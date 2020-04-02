package Pckg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;


public class UDPServer implements Runnable{

	private ServerSocket ssock;
	private int port;
	private static String serverName = "ServeRowena";
	private String IPAddress;
	private Scanner inputScanner;
	private PrintStream outputStream;

	@Override
	public void run() {
		boolean openNewSocket = true;
		while (openNewSocket) {
			try {
				setupSsock();
				Socket sock = ssock.accept();
				printMessage("> Srv: client connected to server.");

				inputScanner = new Scanner(new BufferedInputStream(sock.getInputStream()));
				outputStream = new PrintStream(new BufferedOutputStream(sock.getOutputStream()));

				printMessage("> Server I/O set up.");
				
				while(true) {
				doStuff();
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
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public void doStuff() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		printMessage("Connection still up");
	}

	public void setupSsock() {
		try {
			IPAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			printMessage("Unable to retrieve IP address of localhost!");
			e.printStackTrace();
		}

		ssock = null;
		int maxAttempts = 2;
		int attempts = 0;
		while (ssock == null && attempts < maxAttempts) {
			int port = getPort();
			try {
				printMessage(String.format("Trying to open a socket at %s on port %d", IPAddress, port));
				ssock = new ServerSocket(port, 0, InetAddress.getByName(IPAddress));
				printMessage("Server started at port " + port);
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
		System.out.println(String.format("Welcome to %s, ready to host...", serverName));
		try {
			System.out.println("My IP address is " + InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			System.out.println("Unable to retrieve IP");
			e.printStackTrace();
		}
		new Thread(srv).start();
	}
}
