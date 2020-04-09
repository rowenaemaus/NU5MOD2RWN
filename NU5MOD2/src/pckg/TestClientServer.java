package pckg;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import pckg.UDProtocol.TimeOut;

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
		
		try {
			System.out.println("Hold up a bit...");
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		UDPClient c = new UDPClient();
		new Thread(c).start();
	}
	
}