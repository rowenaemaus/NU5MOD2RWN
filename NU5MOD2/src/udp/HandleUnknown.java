package udp;

public class HandleUnknown implements PacketHandler{

	@Override
	public void handlePkt(UDProtocol p, byte[] data) {
		// Nothing, just receives packet
	}

}
