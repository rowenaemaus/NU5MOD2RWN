package udp;

public class HandleData implements PacketHandler{

	@Override
	public void handlePkt(UDProtocol p, byte[] data) {
		p.printPacketInfo(data);
		int pktNum = p.getPktNum(data);
		p.printMessage(String.format("|| %s sending ack for data packet num: %d", p.name, pktNum));
		p.sendAck(pktNum);
	}

}
