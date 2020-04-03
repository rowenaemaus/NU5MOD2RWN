package Pckg;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class UDPClient implements Runnable{

	private String serverIP;
	private int serverPort;
	private String clientIP;
	private int clientPort;
	private DatagramSocket socket;

	private Scanner in;
	private PrintStream out;

	@Override
	public void run() {
		printMessage("|| Welcome client!\n|| -----------\n");
		setClientIPPort();
		printMessage(String.format("|| Your IP and port are: <%s,%d>", clientIP, clientPort));
		
		// broadcast for connection
		
		askDest();
		
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
		printMessage("|| Client, what file do you want to send to server?\n>");
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

	public void setClientIPPort() {
		try {
			clientIP = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			e.printStackTrace();
			printMessage(e.getMessage());
		}
		Scanner keyboard = new Scanner(System.in);
		System.out.println("|| Client, what port do you want to set your connection?\n>");
		clientPort = keyboard.nextInt();
	}

	public void askDest() {
		printMessage("|| Hello user. What IP is the server you want to connect to on?\n>\b");
		Scanner keyboard = new Scanner(System.in);
//		String ip = keyboard.nextLine();
		String ip = null;
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		printMessage(String.format("|| You entered: %s", ip));
		// TODO check valid ip
		serverIP = ip;
		printMessage("|| And on what port?\n>");
//		int port = keyboard.nextInt();
		int port = 8070;
		printMessage(String.format("|| You entered: %d", port));
		serverPort = port;
		// TODO check valid port
		printMessage(String.format("|| Trying to set up a connection with <%s,%d>", serverIP, serverPort));
	}
	
	public boolean createConnection() {
		clearConnection();

		while (socket == null) {
			try {
//				clientIP = InetAddress.getLocalHost().getHostAddress();
//				clientPort = getIPPort();
				
				// TODO send broadcast
				printMessage(String.format("|| Client on <%s,%d>", clientIP, clientPort));
				printMessage(String.format("|| Server on <%s,%d>", serverIP, serverPort));
				
				socket = new DatagramSocket();
				//				in = new Scanner(new BufferedInputStream(socket.getInputStream()));
				//				out = new PrintStream(new BufferedOutputStream(socket.getOutputStream()));
				printMessage("|| Client socket set up for client!");
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

	public void printMessage(String s) {
		System.out.println(s);
	}

	public String shorter(String s) {
		return (s.length() > 50) ? (s.substring(0,50)+"...") : s; 
	}
	
	public static void main (String[] args) {
		UDPClient c = new UDPClient();
		new Thread(c).start();
	}
}
