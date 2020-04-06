package pckg;

public class UDPClient implements Runnable{
	
	private UDProtocol udp;
	
	@Override
	public void run() {
		printMessage("|| Welcome client!\n|| -----------\n");
		// TODO optional choose port for self
		udp = new UDProtocol("client", 8071);

		createSocket();
		udp.getOthersIP();
		
		printMessage("|| Ready to send packets!");

		String message = "message";
		udp.sendPacket(message.getBytes());

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
