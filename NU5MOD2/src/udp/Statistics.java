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
		printMessage(String.format("***\n Statistics on the transmission of %s:", filename));
		printMessage(String.format(" > Total packets received:\n>> %d packets", pktsReceived));
		printMessage(String.format(" > Total packets sent:\n>> %d packets", pktsSent));
		printMessage(String.format(" > Number of lost packets:\n>> %d packets", lostPackets));
 		printMessage(String.format(" > Number of retransmissions:\n>> %d", retransmit));

		printMessage(String.format(" > Total transmission time:\n>> %d ms", getTotalTime()));
		printMessage(String.format(" > Total file size:\n>> %d", totalFileSize));
		printMessage(String.format(" > Average transfer speed:\n>> %d bytes per second", getSpeed()));
		printMessage("|| ***");
	}

	public int getSpeed() {
		int timeInSec = getTotalTime()/1000;		
		return totalFileSize/timeInSec;
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
