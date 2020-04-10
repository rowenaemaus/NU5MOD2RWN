package pckg;

import java.io.File;
import java.util.Scanner;

public class UDPClient implements Runnable{

	private UDProtocol udp;
	//	String fileName;

	public enum Menu {
		CONTENT ("a", "Show content of server", new RequestOption()),
		REQUEST ("b", "Request file from server", new RequestOption()),
		SEND ("c", "Send a file to server", new SendOption()),
		DELETE ("d", "Delete file from server", new DeleteOption()),
		QUIT ("e", "Shutdown this client", new ShutdownOption());

		public String option;
		public String menuText;
		public MenuOptionInterface handleOption;
		
		private Menu (String option, String menuText, MenuOptionInterface handleOption) {
			this.option = option;
			this.menuText = menuText;
			this.handleOption = handleOption;
		}		
		
		MenuOptionInterface getMenuOption() {
			return handleOption;
		}
		
		String s = "no enclosing instance of type of UDPClient is accessible. Must qualify the allocation with enclosing instance of type UDPClient";
	}

	@Override
	public void run() {
		String home = System.getProperty("user.home");
		File fileLocation = new File(home+"/Downloads/udp"); 

		printMessage("|| Welcome client!\n|| -----------\n");
		// TODO optional choose port for self
		udp = new UDProtocol("client", 8071, fileLocation);

		createSocket();
		udp.multicastSend();
		udp.printMessage(">>>>>>>>>>>>>>>>>");

		printMessage("|| Client ready to go!!");

		udp.sendContentRequest();

		Menu m = printMenu();
		m.handleOption.handleAction();
		
		//		String fileRequest = askFileToGet();
		String fileRequest = "image6.png";

		udp.gimmeFile(fileRequest);


		System.out.println("<<<<<<<<<<<<<<<<<<<");
		System.out.println("U MADE IT THRU BITCH");



		//		File file = new File("/Users/rowena.emaus/nu-module-2/example_files/image1.png");
		//		try {
		//			BufferedReader br = new BufferedReader(new FileReader(file));
		//		} catch (FileNotFoundException e) {
		//			e.printStackTrace();
		//		} 

		//		udp.sendFile(file);

		//		byte[] received = udp.receivePacket();


	}
	
	public Menu printMenu() {
		printMessage("---------------------------");
		printMessage(" MENU:");

		for (Menu m : Menu.values()) {
			printMessage(String.format("- %-10s: %s", m.option, m.menuText));
		}	
		printMessage("---------------------------");
		printMessage(">> Please make your choice:");

		String answer = getAnswer(); 
		Menu validAnswer = checkValidOption(answer);

		while (validAnswer == null) {
			printMessage(">> ERROR: invalid option! Please try again:");
			answer = getAnswer(); 
			validAnswer = checkValidOption(answer);
		}	
		return validAnswer;
	}

	public String askFileToGet() {
		printMessage(">> What file would you like to request?\n...");
		String answer = getAnswer();

		while (!answer.equalsIgnoreCase("exit")) {
			if (!udp.fileListString.contains(answer) && !answer.contains(".")) {
				printMessage(">> WARNING: invalid file request, please try again (or EXIT to cancel)\n...");
				answer = getAnswer();
			} else {
				printMessage(String.format(">> Requesting file '%s' from server...", answer));
				break;
			}
		}
		return answer;
	}

	public Menu checkValidOption(String s) {
		for (Menu m : Menu.values()) {
			if (m.option.equalsIgnoreCase(s)) {
				return m;
			}
		}
		return null;
	}

	public String getAnswer() {
		Scanner keyboard = new Scanner(System.in);
		keyboard.close();
		return keyboard.nextLine();
	}

	
	static class ContentOption implements MenuOptionInterface{
		@Override
		public void handleAction() {
			printMessage("-- Asking server contents --");
			udp.sendContentRequest();
		}
	}
	
	public class RequestOption implements MenuOptionInterface{
		@Override
		public void handleAction() {
			printMessage("-- Requesting file from server --");
			String fileRequest = "image6.png";
			udp.gimmeFile(fileRequest);
		}
	}
	
	public class SendOption implements MenuOptionInterface{
		@Override
		public void handleAction() {
			printMessage("-- Sending file to server --");
			// ask what file
			// udp.sendFile(File filename);
			// TODO Auto-generated method stub
		}
	}
	
	public class DeleteOption implements MenuOptionInterface{
		@Override
		public void handleAction() {
			printMessage("-- Deleting file from server --");
			// TODO Auto-generated method stub
		}
	}
	
	public class ShutdownOption implements MenuOptionInterface{
		@Override
		public void handleAction() {
			printMessage("-- Shutting down client --");
			// TODO Auto-generated method stub
		}
	}
	
	public interface MenuOptionInterface {
		void handleAction();
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

