package pckg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class UDPClient implements Runnable{

	private UDProtocol udp;

	@Override
	public void run() {
		printMessage("|| Welcome client!\n|| -----------\n");
		// TODO optional choose port for self
		udp = new UDProtocol("client", 8071);

		createSocket();
//		udp.getOthersIP();
		udp.multicastSend();
		udp.printMessage(">>>>>>>>>>>>>>>>>");

		printMessage("|| Ready to go!!");

		File file = new File("/Users/rowena.emaus/nu-module-2/example_files/image1.png");
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 

		udp.sendFile(file);

		byte[] received = udp.receivePacket();
	}

	public void createSocket() {
		boolean connected = udp.createSocket();
		for (int i = 0; i < 3; i++) {
			if (!connected) {
				connected = udp.createSocket();
			} else {
				break;
			}
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
