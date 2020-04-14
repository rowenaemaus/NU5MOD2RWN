package pckg;

import java.io.File;

import udp.UDProtocol;


public class UDPServer implements Runnable{
	UDProtocol udp;

	boolean clientConnected = false;

	int maxFiles = 10;
	int numFiles;
	static String fileLocString = "/home/pi/udp2"; 
	// = System.getProperty("user.home")+"/Downloads/udp2";	
	File fileLocation;
	int maxFileSize = 100000;

	@Override
	public void run() {
		File fileLocation = new File(fileLocString); 

		printMessage("|| Welcome server!\n|| -----------\n");
		
		udp = new UDProtocol("server", 8070, fileLocation);

		udp.createSocket();
		udp.multicastReceive();
		udp.printMessage(">>>>>>>>>>>>>>>>>");
		printMessage("|| Server ready to go!");

		udp.getContentRequest();
			
		boolean stop = false;
		while (!stop) {
			udp.receivePacket();
		}
	}

	public void printMessage(String s) {
		System.out.println(s);
	}	

	public static void main (String[] args) {
		UDPServer s = new UDPServer();
		if (args.length > 0) {
			fileLocString = args[0];
		}
		
		new Thread(s).start();
	}
}
