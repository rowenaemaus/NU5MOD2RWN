package udp;

public class Statistics {

	int startTransmission;
	int endTransmission;
	int lostPackets;
	int retransmit;
	int totalFileSize;
	
	
	public Statistics(Object t) {
	}

	public int getStartTransmission() {
		return startTransmission;
	}


	public void setStartTransmission(int startTransmission) {
		this.startTransmission = startTransmission;
	}


	public int getEndTransmission() {
		return endTransmission;
	}


	public void setEndTransmission(int endTransmission) {
		this.endTransmission = endTransmission;
	}


	public int getLostPackets() {
		return lostPackets;
	}


	public void setLostPackets(int lostPackets) {
		this.lostPackets = lostPackets;
	}


	public int getRetransmit() {
		return retransmit;
	}


	public void setRetransmit(int retransmit) {
		this.retransmit = retransmit;
	}


	public int getTotalFileSize() {
		return totalFileSize;
	}


	public void setTotalFileSize(int totalFileSize) {
		this.totalFileSize = totalFileSize;
	}
	
	
	
	
}
