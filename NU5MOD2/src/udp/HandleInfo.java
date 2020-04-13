package udp;

public class HandleInfo implements PacketHandler{

	@Override
	public void handlePkt(UDProtocol p, byte[] data) {
		p.printPacketInfo(data);

		byte[] list = new byte[data.length-UDProtocol.HEADERSIZE];
		System.arraycopy(data, UDProtocol.HEADERSIZE, list, 0, data.length-UDProtocol.HEADERSIZE);
		p.setFileListString(new String(list));
		p.printMessage(String.format("|| List of contents of <%s;%d>:", p.getOtherIP().getCanonicalHostName(), p.getOtherPort()));
		p.printMessage("--\n"+p.getFileListString()+"--");
	}
}
