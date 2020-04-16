package pckg;

import java.io.File;

import udp.UDProtocol;


public class UDPServer implements Runnable{
	UDProtocol udp;

	boolean clientConnected = false;
	 
	private static String fileLocString = "/home/pi/udp2";
	
//	private static String fileLocString = System.getProperty("user.home")+"/Downloads/udp2";	

	@Override
	public void run() {
		File fileLocation = new File(fileLocString); 

		printMessage("|| Welcome server!\n|| -----------");
		
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
