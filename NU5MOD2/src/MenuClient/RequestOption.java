package MenuClient;

import java.util.ArrayList;
import java.util.List;

import pckg.UDPClient;
import udp.UDProtocol;

public class RequestOption implements MenuOptionInterface{

	private UDPClient c;
	private UDProtocol p;
	private List<String> fileRequest = new ArrayList<String>();

	@Override
	public void handleAction(UDPClient c) {
		this.c = c;
		this.p = c.getUdp();
		c.printMessage("-- Requesting file from server --");
		
		fileRequest.clear();
		askFilesToGet();

		if (fileRequest.isEmpty()) { 
			c.printMessage(">> Nothing to receive. Cancelling file request,");
			return;
		}
		
		for (String s : fileRequest) {
			p.gimmeFile(s);
		}
	}

	public void askFilesToGet() {
		c.printMessage(">> What file(s) would you like to request? (Please enter with spaces in between)\n...");
		String answer = c.getAnswer();

		String[] answers = answer.split(" ");

		for (String s : answers) {
			if (!p.getFileListString().contains(answer) && !answer.contains(".")) {
				c.printMessage(String.format(">> WARNING: invalid file '%s', not requesting...", s));
			} else {
				c.printMessage(String.format(">> Adding %s to request from server...", s));
				fileRequest.add(s);
			}
		}

		c.printMessage(String.format(">> You are about to request: %s. Do you want to proceed (yes/no)?", fileRequest.toString()));
		answer = c.getAnswer();

		while (!(answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("no"))) {
			c.printMessage(">> WARNING: Invalid response, please try again\n...");
			answer = c.getAnswer();
		}
		
		switch (answer) {
		case "yes":
			return;
		case "no:":
			fileRequest.clear();
			return;
		}
	}	
}