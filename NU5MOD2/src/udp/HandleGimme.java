package udp;

public class HandleGimme implements PacketHandler{
	
	@Override
	public void handlePkt(UDProtocol p, byte[] data) {
		p.printPacketInfo(data);
		int size = p.getPktNum(data);
		byte[] fileRequest = new byte[size];
		System.arraycopy(data, UDProtocol.HEADERSIZE, fileRequest, 0, size);

		String filename = new String(fileRequest);
		if (p.getAvailableFiles().contains(filename)) {
			p.printMessage(String.format("|| %s has %s available. Sending ack for file transfer", p.name, filename));
			p.sendAck(0);
			p.sendFile(p.setFile(filename));
		} else {
			p.printMessage(String.format("|| %s declines. %s not available in file list", p.name, filename));
			p.sendDecline();
		}	
		
		p.printMessage("|| Curent file list:");
		p.printMessage(p.getContentlistString());
		p.printMessage("|| -----------------");
	}
}
