package udp;

public class Statistics {

	private String filename;
	private long startTransmission;
	private long endTransmission;
	private int pktsReceived;
	private int pktsSent;
	private int lostPackets;
	private int retransmit;
	private int totalFileSize;


	public Statistics(String filename) {
		this.filename = filename;
		this.startTransmission = 0;
		this.endTransmission = 0;
		this.pktsReceived = 0;
		this.pktsSent = 0;
		this.lostPackets = 0;
		this.retransmit = 0;
		this.totalFileSize = 0;
	}

	public void printStats() {
		printMessage(String.format("***** Statistics on the transmission of %s:", filename));
		printMessage(String.format("%-32s%10d %16s", "Total packets received:",pktsReceived, "packets"));
		printMessage(String.format("%-32s%10d %16s", "Total packets sent:",pktsSent, "packets"));
		printMessage(String.format("%-32s%10d %16s", "Total lost packets:",lostPackets, "packets"));
		printMessage(String.format("%-32s%10d %16s", "Total retransmissions:",retransmit, "packets"));
		printMessage(String.format("%-32s%10d %16s", "Total transmission time:",getTotalTime(), "ms"));
		printMessage(String.format("%-32s%10d %16s", "Total file size:",totalFileSize, "bytes"));
		printMessage(String.format("%-32s%10.3f %16s", "Average transfer speed:",getSpeed(), "bytes per ms"));
		printMessage("*****");
	}

	public double getSpeed() {		
		return totalFileSize/getTotalTime();
	}
	
	
	public long getStartTransmission() {
		return startTransmission;
	}


	public void setStartTransmission(long startTransmission) {
		this.startTransmission = startTransmission;
	}


	public long getEndTransmission() {
		return endTransmission;
	}


	public void setEndTransmission(long endTransmission) {
		this.endTransmission = endTransmission;
	}

	public int getTotalTime() {
		return (int) (endTransmission - startTransmission);
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


	public int getPktsReceived() {
		return pktsReceived;
	}

	public void setPktsReceived(int pktsReceived) {
		this.pktsReceived = pktsReceived;
	}

	public int getPktsSent() {
		return pktsSent;
	}

	public void setPktsSent(int pktsSent) {
		this.pktsSent = pktsSent;
	}

	public void addReceived() {
		pktsReceived++;
	}

	public void addSent() {
		pktsSent++;
	}

	public void addRetransmit() {
		this.retransmit++;
	}

	public void printMessage(String s) {
		System.out.println(s);
	}

}
