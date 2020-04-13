package MenuClient;

import pckg.UDPClient;
import udp.UDProtocol;
import udp.UDProtocol.HeaderIdx;
import udp.UDProtocol.PktType;

public class SendOption implements MenuOptionInterface{
	private UDPClient c;
	private UDProtocol p;
	
	@Override
	public void handleAction(UDPClient c) {
		this.c = c;
		this.p = c.getUdp();
		c.printMessage("-- Sending file to server --");

		c.getUdp().updateContentList();
		c.printMessage(">> Your file list is: " + c.getUdp().getAvailableFiles().toString());

		String fileToSend = askFilename();
		
		// send file pkt to let server know whatsup
		sendFilePkt(fileToSend);
		c.getUdp().sendFile(c.getUdp().setFile(fileToSend));
	}

	private String askFilename() {
		c.printMessage(">> What file would you like to send?\n...");
		String answer = c.getAnswer();

		while (!answer.equalsIgnoreCase("exit")) {
			if (!c.getUdp().getAvailableFiles().contains(answer)) {
				c.printMessage(">> WARNING: invalid file request, please try again (or EXIT to cancel)\n...");
				answer = c.getAnswer();
			} else {
				c.printMessage(String.format(">> Sending file '%s' to server...", answer));
				break;
			}
		}
		return answer;
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