package Pckg;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class UDPClient implements Runnable{

	// server address
	private String host;
	// server port
	private int port;
	// server socket
	private DatagramSocket socket;

	// scanner over socket input stream
	private Scanner in;

	// socket output stream
	private PrintStream out;

	@Override
	public void run() {
		boolean connected = createConnection();
		for (int i = 0; i < 3; i++) {
			if (!connected) {
				connected = createConnection();
			} else {
				break;
			}
		}

		// select file
		String filename = selectFile();
		printMessage("|| Client: selected file");
		
		// read file in
		Integer[] fileContents = readFile(filename);
		printMessage("|| Client: file read");
		
		// make into packets
		
		// send the packets


	}

	public String selectFile() {
		// TODO pick file
		printMessage("> Client, what file do you want to send to server?");
		return ("src/image1.png");
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
			printMessage("ERROR: Client unable to setup filestream");
			return null;
		}
	}

	public void printMessage(String s) {
		System.out.println(s);
	}

	public String shorter(String s) {
		return (s.length() > 50) ? (s.substring(0,50)+"...") : s; 
	}

	public int askPort() {
		Scanner keyboard = new Scanner(System.in);
		System.out.println("> Client, what port do you want to set a connection on?");
		// TODO
		return 8071;
	}

	public boolean createConnection() {
		clearConnection();

		while (socket == null) {
			try {
				host = InetAddress.getLocalHost().getHostAddress();
				port = askPort();
				printMessage(String.format("|| Client attempting to connect to port %d on %s", port, host));
				socket = new DatagramSocket(port);
				//				in = new Scanner(new BufferedInputStream(socket.getInputStream()));
				//				out = new PrintStream(new BufferedOutputStream(socket.getOutputStream()));
				printMessage("|| Client socket set up for client!");
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
		printMessage("|| Client closing the connection...");
		try {
			in.close();
			out.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main (String[] args) {
		UDPClient c = new UDPClient();
		new Thread(c).start();
	}
}
