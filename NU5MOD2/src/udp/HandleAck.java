package udp;

import java.util.Arrays;

public class HandleAck implements PacketHandler {

	@Override
	public void handlePkt(UDProtocol p, byte[] data) {
		data = Arrays.copyOfRange(data, 0, UDProtocol.HEADERSIZE);
		p.printPacketInfo(data);
		p.acksReceived.add(p.getPktNum(data));		
	}
}
