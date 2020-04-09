package pckg;

import java.io.File;
import java.util.Scanner;

public class UDPClient implements Runnable{

	private UDProtocol udp;
//	String fileName;
	
	@Override
	public void run() {
		String home = System.getProperty("user.home");
		File fileLocation = new File(home+"/Downloads/"); 
		
		printMessage("|| Welcome client!\n|| -----------\n");
		// TODO optional choose port for self
		udp = new UDProtocol("client", 8071, fileLocation);

		createSocket();
//		udp.getOthersIP();
		udp.multicastSend();
		udp.printMessage(">>>>>>>>>>>>>>>>>");

		printMessage("|| Client ready to go!!");

		udp.sendContentRequest();
		
		String fileRequest = askFileToGet();
		
		udp.gimmeFile(fileRequest);
		
		
		
		
		
		
//		File file = new File("/Users/rowena.emaus/nu-module-2/example_files/image1.png");
//		try {
//			BufferedReader br = new BufferedReader(new FileReader(file));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} 

//		udp.sendFile(file);

		byte[] received = udp.receivePacket();
	}

	public String askFileToGet() {
		Scanner keyboard = new Scanner(System.in);
		printMessage(">> What file would you like to request?\n...");
		String answer = keyboard.nextLine();
		
		if (!udp.fileListString.contains(answer) && !answer.contains(".")) {
			printMessage(">> WARNING: invalid file request, please try again (or EXIT to cancel)\n...");	
		} else {
			printMessage(">> Requesting file 'answer' from server...");
		}
		keyboard.close();
		return answer;
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

