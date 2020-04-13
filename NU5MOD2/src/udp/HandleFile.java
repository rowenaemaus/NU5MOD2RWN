package udp;

public class HandleFile implements PacketHandler{
	UDProtocol p;

	@Override
	public void handlePkt(UDProtocol p, byte[] data) {
		this.p = p;
		p.printPacketInfo(data);	
		
		//TODO als teveel files al, dan weiger (decline pkt)
		
		int size = p.getPktNum(data);
		byte[] fileRequest = new byte[size];
		System.arraycopy(data, UDProtocol.HEADERSIZE, fileRequest, 0, size);

		String filename = new String(fileRequest);
		p.receiveFile(filename);
	}
}
