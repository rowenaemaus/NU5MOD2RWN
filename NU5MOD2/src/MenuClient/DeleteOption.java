package MenuClient;

import pckg.UDPClient;

public class DeleteOption implements MenuOptionInterface{
	private UDPClient c;

	@Override
	public void handleAction(UDPClient c) {
		this.c = c;
		System.out.println("-- Deleting file from server --");
		String deleteRequest = askFileToDelete();
		if (!deleteRequest.equalsIgnoreCase("exit")) {
			c.getUdp().deleteFile(deleteRequest);
		}
	}

	public String askFileToDelete() {
		c.printMessage(">> What file would you like to delete from the server?\n...");
		String answer = c.getAnswer();

		while (!answer.equalsIgnoreCase("exit")) {
			if (!c.getUdp().getFileListString().contains(answer)) {
				c.printMessage(">> WARNING: invalid file request, please try again (or EXIT to cancel)\n...");
				answer = c.getAnswer();
			} else {
				c.printMessage(String.format(">> Requesting to delete file '%s' from server...", answer));
				if (!areYouSure()) {
					answer = "exit";
				}
				break;
			}
		}
		return answer;
	}

	public boolean areYouSure() {
		c.printMessage(">> Are you sure? YES/NO\n...");
		String answer = c.getAnswer();
		return answer.equalsIgnoreCase("yes") ? true : false;
	}

}
