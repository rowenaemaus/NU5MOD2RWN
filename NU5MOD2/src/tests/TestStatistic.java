package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import udp.Statistics;

public class TestStatistic {

	private Statistics s;
	private final PrintStream originalOut = System.out;
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	
	@Before
	public void setUp() {
		s = new Statistics("filename");
	}
	
	@Test
	public void testPrint() {
		System.setOut(new PrintStream(outContent));
		s.printStats();
		
		assertTrue(outContent.toString().contains(String.format("%-32s%10d %16s", "Total packets received:",s.getPktsReceived(), "packets")));
		assertTrue(outContent.toString().contains(String.format("%-32s%10d %16s", "Total packets sent:",s.getPktsSent(), "packets")));
		assertTrue(outContent.toString().contains(String.format("%-32s%10d %16s", "Total lost packets:",s.getLostPackets(), "packets")));
		assertTrue(outContent.toString().contains(String.format("%-32s%10d %16s", "Total retransmissions:",s.getRetransmit(), "packets")));
		assertTrue(outContent.toString().contains(String.format("%-32s%10d %16s", "Total transmission time:",s.getTotalTime(), "ms")));
		assertTrue(outContent.toString().contains(String.format("%-32s%10d %16s", "Total file size:",s.getTotalFileSize(), "bytes")));
		assertTrue(outContent.toString().contains(String.format("%-32s%10.3f %16s", "Average transfer speed:",s.getSpeed(), "bytes per ms")));
	}
	
	@Test
	public void testTime() {
		int starttime = 0;
		int endtime = 10;
		s.setStartTransmission(starttime);
		s.setEndTransmission(endtime);
		assertEquals(s.getTotalTime(), (endtime-starttime));
	}
	
	@Test
	public void testAddReceivedPkt() {
		int numPktReceived = s.getPktsReceived();
		assertEquals(numPktReceived, 0);
		s.addReceived();
		assertEquals(s.getPktsReceived(),numPktReceived+1);
	}
	
	@Test
	public void testAddSentPkt() {
		int numPktSent = s.getPktsSent();
		assertEquals(numPktSent, 0);
		s.addSent();
		assertEquals(s.getPktsSent(),numPktSent+1);
	}	
	
	@After
	public void restoreStreams() {
		System.setOut(originalOut);
	}
}
