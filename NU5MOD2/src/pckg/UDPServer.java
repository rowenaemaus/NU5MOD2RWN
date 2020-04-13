package pckg;

import java.io.File;

import udp.UDProtocol;


public class UDPServer implements Runnable{
	UDProtocol udp;

	boolean clientConnected = false;

	int maxFiles = 10;
	int numFiles;
	File fileLocation = new File("src/filesRowena");
	int maxFileSize = 100000;

	@Override
	public void run() {
		printMessage("|| Welcome server!\n|| -----------\n");
		
		udp = new UDProtocol("server", 8070, fileLocation);

		udp.multicastReceive();
//		udp.createSocket();
		udp.printMessage(">>>>>>>>>>>>>>>>>");
		printMessage("|| Server ready to go!");

		udp.getContentRequest();
			
		boolean stop = false;
		while (!stop) {
			udp.receivePacket();
		}
	}

	public void checkMemory(byte[] data) {
		// TODO check if max files is reached. Otherwise refuse to save.	
	}

	public void printMessage(String s) {
		System.out.println(s);
	}	

	public static void main (String[] args) {
		UDPServer s = new UDPServer();
		new Thread(s).start();
	}
}
