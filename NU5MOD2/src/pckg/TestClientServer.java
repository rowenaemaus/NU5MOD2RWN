package pckg;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class TestClientServer {

	public static void main(String[] args) {
		
		UDPServer srv = new UDPServer();
		System.out.println(String.format(">> Welcome to %s, ready to host...", "Rowena"));
		try {
			System.out.println(">> IP address is " + InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			System.out.println("ERROR: Unable to retrieve IP");
			e.printStackTrace();
		}
		new Thread(srv).start();
		
		UDPClient c = new UDPClient();
		new Thread(c).start();
	}
	
}
