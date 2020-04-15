package MenuClient;

import pckg.UDPClient;
import udp.UDProtocol;

public class RequestOption implements MenuOptionInterface{
	
	private UDPClient c;
	private UDProtocol p;
	
	@Override
	public void handleAction(UDPClient c) {
		this.c = c;
		this.p = c.getUdp();
		c.printMessage("-- Requesting file from server --");
		String fileRequest = askFileToGet();
		p.gimmeFile(fileRequest);
	}
	
	public String askFileToGet() {
		c.printMessage(">> What file would you like to request?\n...");
		String answer = c.getAnswer();
		
		while (!answer.equalsIgnoreCase("exit")) {
			if (!p.getFileListString().contains(answer) && !answer.contains(".")) {
				c.printMessage(">> WARNING: invalid file request, please try again (or EXIT to cancel)\n...");
				answer = c.getAnswer();
			} else {
				c.printMessage(String.format(">> Requesting file '%s' from server...", answer));
				break;
			}
		}
		return answer;
	}	
}