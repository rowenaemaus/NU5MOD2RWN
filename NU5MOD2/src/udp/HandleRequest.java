package udp;

import java.util.Arrays;

import udp.UDProtocol.HeaderIdx;
import udp.UDProtocol.PktType;

public class HandleRequest implements PacketHandler{

	@Override
	public void handlePkt(UDProtocol p, byte[] data) {
		data = Arrays.copyOfRange(data, 0, UDProtocol.HEADERSIZE);
		p.printPacketInfo(data);

		String contents = p.getContentlistString();
		p.printMessage(String.format("|| %s has %d files", p.name, p.getFileList().length));

		byte[] pkt = new byte[UDProtocol.HEADERSIZE+contents.getBytes().length];
		pkt[HeaderIdx.TYPE.value] = (byte) PktType.INFO.value;
		System.arraycopy(contents.getBytes(), 0, pkt, UDProtocol.HEADERSIZE, contents.getBytes().length);

		p.printMessage(String.format("|| %s sent packet listing all files in '%s'", p.name, p.getFileLocation().getName()));
		p.sendPacket(pkt);
		
	}
}
