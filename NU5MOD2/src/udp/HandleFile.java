package udp;

public class HandleFile implements PacketHandler{
	UDProtocol p;

	@Override
	public void handlePkt(UDProtocol p, byte[] data) {
		this.p = p;
		p.printPacketInfo(data);	
		
		if (p.getAvailableFiles().size() < p.getMaxFiles()) {
			int size = p.getPktNum(data);
			byte[] fileRequest = new byte[size];
			System.arraycopy(data, UDProtocol.HEADERSIZE, fileRequest, 0, size);

			String filename = new String(fileRequest);
			p.sendAck(0);
			p.receiveFile(filename);
		} else {
			p.sendDecline();
			p.printMessage("|| WARNING: max file number reached. Not saving file...");
		}
	}
}
