package pckg;

import java.io.File;
import java.util.Scanner;

import MenuClient.MenuOptionInterface;
import udp.UDProtocol;

public class UDPClient implements Runnable{

	private UDProtocol udp;
	private Scanner keyboard = new Scanner(System.in);
	private static String fileLocString = System.getProperty("user.home")+"/Downloads/udp";
	
	public enum Menu {
		CONTENT ("a", "Show content of server", new MenuClient.ContentOption()),
		REQUEST ("b", "Request file from server", new MenuClient.RequestOption()),
		SEND ("c", "Send a file to server", new MenuClient.SendOption()),
		DELETE ("d", "Delete file from server", new MenuClient.DeleteOption()),
		QUIT ("e", "Shutdown this client", new MenuClient.ShutdownOption());

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
	}

	public UDPClient(String name, int port) {
		File fileLocation = new File(fileLocString);

		printMessage("|| Welcome client!\n|| -----------\n");
		this.udp = new UDProtocol(name , port, fileLocation);
	}
	
	@Override
	public void run() {
		createSocket();
		getUdp().multicastSend();
		printMessage(">>>>>>>>>>>>>>>>>");

		printMessage("|| Client ready to go!!");

		getUdp().sendContentRequest();

		Menu m = null;

		while (m != Menu.QUIT) {
			printMenu();
			m = pickMenu();
			m.handleOption.handleAction(this);
		}
		printMessage("<<<<<<<<<<<<<<<<<");
		printMessage(">> Thanks, bye!");
		keyboard.close();
	}

	public void printMenu() {
		printMessage("---------------------------");
		printMessage(" MENU:");

		for (Menu m : Menu.values()) {
			printMessage(String.format("- %-10s: %s", m.option, m.menuText));
		}	
	}

	public Menu pickMenu() {
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
		
	public Menu checkValidOption(String s) {
		for (Menu m : Menu.values()) {
			if (m.option.equalsIgnoreCase(s)) {
				return m;
			}
		}
		return null;
	}

	public String getAnswer() {
		return keyboard.nextLine(); 
	}

	public void createSocket() {
		boolean connected = getUdp().createSocket();
		for (int i = 0; i < 3; i++) {
			if (!connected) {
				connected = getUdp().createSocket();
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
		UDPClient c = new UDPClient("client", 8071);
		if (args.length > 0) {
			fileLocString = args[0];
		}

		new Thread(c).start();
	}
	
	public UDProtocol getUdp() {
		return udp;
	}
}

