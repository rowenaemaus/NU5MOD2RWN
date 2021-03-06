package menuClient;

import java.util.ArrayList;
import java.util.List;

import pckg.UDPClient;
import udp.UDProtocol;
import udp.UDProtocol.HeaderIdx;
import udp.UDProtocol.PktType;

public class SendOption implements MenuOptionInterface{
	private UDPClient c;
	private UDProtocol p;
	private List<String> filesToSend = new ArrayList<String>();

	@Override
	public void handleAction(UDPClient c) {
		this.c = c;
		this.p = c.getUdp();
		c.printMessage("-- Sending file to server --");

		p.updateContentList();
		c.printMessage(">> Your file list is: " + p.getAvailableFiles().toString());

		filesToSend.clear();
		askFilesToSend();
		if (filesToSend.isEmpty()) { 
			c.printMessage(">> Nothing to send. Cancelling file request.");
			return;
		}

		for (String filename : filesToSend) {
			sendFilePkt(filename);
			byte[] response = p.receivePacket();
			PktType responseType = p.getPktType(response);

			if (responseType == PktType.ACK) {
				c.printMessage(String.format(">> Sending file: '%s'", filename));
				p.sendFile(p.setFile(filename));
			} else {
				c.printMessage(String.format("WARNING: Not sending %s. Max number of files on server reached", filename));
			}
		}
	}

	public void askFilesToSend() {
		c.printMessage(">> What file(s) would you like to send? (Please enter with spaces in between)\n...");
		String answer = c.getAnswer();

		String[] answers = answer.split(" ");

		for (String s : answers) {
			if (!p.getAvailableFiles().contains(s)) {
				c.printMessage(String.format(">> WARNING: invalid file '%s', not sending...", s));
			} else {
				c.printMessage(String.format(">> Adding %s to send-list to server...", s));
				filesToSend.add(s);	
			}
		}

		c.printMessage(String.format(">> You are about to send: %s. Do you want to proceed (yes/no)?", filesToSend.toString()));
		answer = c.getAnswer();

		while (!(answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("no"))) {
			c.printMessage(">> WARNING: Invalid response, please try again\n...");
			answer = c.getAnswer();
		}

		switch (answer) {
		case "yes":
			return;
		case "no:":
			filesToSend.clear();
			return;
		}
	}

	private void sendFilePkt(String filename) {
		byte[] pkt = new byte[UDProtocol.HEADERSIZE + filename.getBytes().length];
		pkt[HeaderIdx.TYPE.value] = (byte) PktType.FILE.value;
		pkt[HeaderIdx.PKTNUM.value] = (byte) filename.getBytes().length;
		System.arraycopy(filename.getBytes(), 0, pkt, UDProtocol.HEADERSIZE, filename.getBytes().length);

		c.printMessage(String.format("|| %s intents to send file '%s'", p.name, filename));
		p.sendPacket(pkt);
	}	
}