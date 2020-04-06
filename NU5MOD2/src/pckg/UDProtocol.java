package pckg;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDProtocol {

	DatagramSocket socket = null;
	private InetAddress ownIP;
	private int ownPort;

	private InetAddress otherIP = null;
	private int otherPort = -1;

	String name;

	public UDProtocol(String name, int port) {
		try {
			this.ownIP = InetAddress.getLocalHost();
			this.ownPort = port;
			this.name = name;
			printMessage(String.format("|| Your IP and port are: <%s,%d>", ownIP.getHostAddress(), ownPort));
		} catch (Exception e) {
			e.printStackTrace();
			printMessage(e.toString());
			printMessage("ERROR: Unable to retrieve IP of localhost!");
		}
	}

	public void getOthersIP() {
		//TODO implement broadcast etc
		setOtherIP(ownIP);
		if (name.equalsIgnoreCase("client")) {
			setOtherPort(8070);
		} else {
			setOtherPort(8071);
		}
	}

	public void sendPacket(byte[] data) {
		try {
			DatagramPacket send = new DatagramPacket(data, data.length, otherIP, otherPort);
			printMessage(String.format("|| %s trying to send packet <%s>", name, new String(data)));
			socket.send(send);
			printMessage(String.format("|| SUCCESS: %s sent packet", name));
		} catch (Exception e) {
			e.printStackTrace();
			printMessage("ERROR: sending packet not succesfull");
		}
	}

	public byte[] receivePacket() {
		try {
			byte[] buffer = new byte[65535];
			DatagramPacket received = new DatagramPacket(buffer, buffer.length);
			socket.receive(received);
			printInfo(received);

			//			String data = new String(request.getData());
			printMessage(String.format("|| Incoming data on %s: %s", name, new String(received.getData())));

			if (otherIP == null && otherPort == -1) {
				setIPFromPkt(received);
			}

			return received.getData();
			// TODO extract data
			// save this data
			// if memory full block it

		} catch (Exception e) {
			e.printStackTrace();
			printMessage("ERROR: unable to receive packet");
		}
		return null;
		//TODO refuse packets if max storage
	}

	public boolean createSocket() {
		int maxAttempts = 2;
		int attempts = 0;

		while (socket == null && attempts < maxAttempts) {
			try { 
				printMessage(String.format("|| Trying to set up socket for %s on <%s,%d>", name, ownIP.getHostName(), ownPort));
				socket = new DatagramSocket(ownPort);
				printMessage(String.format("|| %s socket set up!", name));
			} catch (Exception e) {
				printMessage(String.format("WARNING: Attempt %d: could not create socket on port %d", attempts, ownPort));
				attempts++;
			}
		}
		return (socket != null);
	}

	public void clearConnection() {
		socket = null;
	}

	public void closeConnection() {
		printMessage(String.format("|| %s closing the connection...", name));
		try {
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setIPFromPkt(DatagramPacket p) {
		printMessage("|| Setting IP and port from received packet...");
		otherIP = p.getAddress();
		otherPort = p.getPort();
		printMessage(String.format("|| Destination IP and port are: <%s,%d>", otherIP.toString(), otherPort));
	}

	public void printInfo(DatagramPacket p) {
		printMessage(String.format("|| Packet info: ip %s, port %d", p.getAddress().toString(), p.getPort()));
	}

	public void printMessage(String s) {
		System.out.println(s);
	}

	public String shorter(String s) {
		return (s.length() > 50) ? (s.substring(0,50)+"...") : s; 
	}

	public InetAddress getOwnIP() {
		return ownIP;
	}

	public int getOwnPort() {
		return ownPort;
	}

	public void setOwnPort(int ownPort) {
		this.ownPort = ownPort;
	}

	public InetAddress getOtherIP() {
		return otherIP;
	}
	
	public void setOtherIP(InetAddress ip) {
		this.otherIP = ip;
	}

	public int getOtherPort() {
		return otherPort;
	}
	
	public void setOtherPort(int port) {
		this.otherPort = port;
	}

	public String getName() {
		return name;
	}
	
	public DatagramSocket getSocket() {
		return this.socket;
	}
}

