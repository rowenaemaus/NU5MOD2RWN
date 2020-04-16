package udp;

import java.io.File;

public class HandleDelete implements PacketHandler {
	
	@Override
	public void handlePkt(UDProtocol p, byte[] data) {
		p.printPacketInfo(data);
		int size = p.getPktNum(data);
		byte[] deleteRequest = new byte[size];
		
		System.arraycopy(data, UDProtocol.HEADERSIZE, deleteRequest, 0, size);
		
		String filename = new String(deleteRequest);
		if (p.getAvailableFiles().contains(filename)) {
			p.printMessage(String.format("|| %s has %s available. Trying to delete...", p.name, filename));
			p.sendAck(0);
			File f = p.setFile(filename);
			boolean success = p.deleteFromServer(f);
			if (success) {
				p.printMessage(String.format("|| %s made it! File is gone.", p.name));
				p.sendAck(0);
				p.updateContentList();
			} else {
				p.printMessage(String.format("|| %s failed! File not deleted.", p.name));
				p.sendDecline();
			}
		} else {
			p.printMessage(String.format("|| %s declines. %s is not available in file list", p.name, filename));;
			p.sendDecline();
		}
	}
}
