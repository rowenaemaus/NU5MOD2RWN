package Pckg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class UDPClient implements Runnable{

	// server address
	private String host;
	// server port
	private int port;
	// server socket
	private Socket socket;

	// scanner over socket input stream
	private Scanner in;

	// socket output stream
	private PrintStream out;

	public void sendFile() {
		// get file contents
		// cut up the file in snippets
		// make packets using DatagramPacket
		// send packet
		//		DatagramPacket dp = new DatagramPacket();

	}

	public int askPort() {
		Scanner keyboard = new Scanner(System.in);
		System.out.println("> Client, what port do you want to set a connection on?");
		return keyboard.nextInt();

	}

	@Override
	public void run() {
		createConnection();

		// connect to server
		// run sendFile oid
	}

	public void printMessage(String s) {
		System.out.println(s);
	}

	public String shorter(String s) {
		return (s.length() > 50) ? (s.substring(0,50)+"...") : s; 
	}

	public boolean createConnection() {
		clearConnection();

		while (socket == null) {
			try {
				host = InetAddress.getLocalHost().getHostAddress();
				port = askPort();
				printMessage(String.format("Attempting to connect to port %d on %s", port, host));
				socket = new Socket(host, port);
				in = new Scanner(new BufferedInputStream(socket.getInputStream()));
				out = new PrintStream(new BufferedOutputStream(socket.getOutputStream()));
				printMessage("I/O set up for client!");
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(e);
				return false;
			}
		}
		return (socket != null);
	}

	public void clearConnection() {
		socket = null;
		in = null;
		out = null;
	}

	public void closeConnection() {
		System.out.println("Closing the connection...");
		try {
			in.close();
			out.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main (String[] args) {
		UDPClient c = new UDPClient();
		new Thread(c).start();
	}
}
