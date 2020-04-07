package pckg;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class UDPServer implements Runnable{
	private static String serverName = "Serverowena";
	UDProtocol udp;

	boolean clientConnected = false;

	int maxFiles = 10;
	int numFiles;
	File fileLocation = new File("src/");
	int maxFileSize = 100000;

	@Override
	public void run() {
		printMessage("|| Welcome server!\n|| -----------\n");
		// TODO optional choose port for self
		udp = new UDProtocol("server", 8070);

		udp.createSocket();
		udp.getOthersIP();

		byte[] fileToReceive = new byte[maxFileSize];
		printMessage("|| Ready to receive packets...");

		udp.receiveFile(); // wordt opgeslagen in /src
//		udp.writeByte(fileToReceive);

		//		String servermsg = "srv";
		//		udp.sendPacket(servermsg.getBytes());

	}

	public void checkMemory(byte[] data) {
		// TODO check if max files is reached. Otherwise refuse to save.	
	}

	public int countFiles() {
		File folder = new File("src/");
		return folder.listFiles().length;
	}

	public void printFiles() {
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


	public void printMessage(String s) {
		System.out.println(s);
	}	

	public static void main (String[] args) {
//				UDPServer srv = new UDPServer();
//				System.out.println(String.format(">> Welcome to %s, ready to host...", serverName));
//				try {
//					System.out.println(">> IP address is " + InetAddress.getLocalHost().getHostAddress());
//				} catch (UnknownHostException e) {
//					System.out.println("ERROR: Unable to retrieve IP");
//					e.printStackTrace();
//				}
//				new Thread(srv).start();
//				
//				
				UDPServer s = new UDPServer();
				new Thread(s).start();
	}
}
