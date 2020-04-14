package MenuClient;

import pckg.UDPClient;

public class RequestOption implements MenuOptionInterface{
	
	private UDPClient c;
	
	@Override
	public void handleAction(UDPClient c) {
		this.c = c;
		c.printMessage("-- Requesting file from server --");
		//		String fileRequest = "image4.png"; // for testing
		String fileRequest = askFileToGet();
		c.getUdp().gimmeFile(fileRequest);
	}
	
	public String askFileToGet() {
		c.printMessage(">> What file would you like to request?\n...");
		String answer = c.getAnswer();
		
		while (!answer.equalsIgnoreCase("exit")) {
			if (!c.getUdp().getFileListString().contains(answer) && !answer.contains(".")) {
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