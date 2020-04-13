package udp;

public interface PacketHandler {
	
	public void handlePkt(UDProtocol p, byte[] data);

}
