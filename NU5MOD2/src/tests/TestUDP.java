package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import org.junit.After;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import udp.HandleAck;
import udp.HandleData;
import udp.HandleRequest;
import udp.UDProtocol;
import udp.UDProtocol.PktType;


public class TestUDP {

	private static UDProtocol udp;
	private static String name = "name";
	private static int port = 5050;

	private final PrintStream originalOut = System.out;
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();


	@BeforeAll
	public static void setUp() {
		udp = new UDProtocol(name, port, null);
	}

	@Test
	public void testSetName() {
		assertEquals(udp.getName(), name);
	}

	@Test
	public void testSetPort() {
		assertEquals(udp.getOwnPort(), port);
	}

	@Test
	public void testSetIP() {
		assertTrue(udp.getOwnIP().toString().contains("192.168"));
	}

	@Test
	public void testSetSocket() {
		udp.createSocket();
		assertNotNull(udp.getSocket());

		udp.closeConnection();
		udp.clearConnection();
		assertNull(udp.getSocket());
	}

	@Test
	public void testPktType() {
		PktType type = PktType.DATA;
		int value = PktType.DATA.value;
		assertEquals(type, PktType.getType(value));
	}

	@Test
	public void testPktTypeHandler() {
		PktType type = PktType.DATA;
		assertTrue(type.getHandler() instanceof HandleData);

		type = PktType.ACK;
		assertTrue(type.getHandler() instanceof HandleAck);

		type = PktType.REQUEST;
		assertTrue(type.getHandler() instanceof HandleRequest);
	}

	@Test
	public void testDelete() {
		System.setOut(new PrintStream(outContent));
		File f = new File("name");
		udp.deleteFromServer(f);
		assertFalse(udp.deleteFromServer(f));
		assertTrue(outContent.toString().contains("|| FAILURE:"));
	}

	@Test
	public void testDataPkt() {
		File fileLocation = new File(System.getProperty("user.home"));
		UDProtocol udp2 = new UDProtocol("name2", 5051, fileLocation); 

		udp.createSocket();
		udp.setOtherPort(5051);
		udp.setOtherIP(udp.getOwnIP());

		udp2.createSocket();
		udp2.setOtherPort(5050);
		udp2.setOtherIP(udp2.getOwnIP());

		byte[] send = name.getBytes();
		udp2.sendPacket(send);

		byte[] received = udp.receivePacket();

		String sendString = new String(send);
		String receivedString = new String(received);
		assertTrue(receivedString.contains(sendString));
	}

	@Test
	public void testPktsIn() {
		int start = 0;
		int end = 1;
		assertFalse(udp.allPktsIn(start, end));
	}

	@Test
	public void testGetNetIp() {
		String localIP = udp.getNetworkIP().getHostAddress();
		String localPrefix = "192.168.";
		assertTrue(localIP.contains(localPrefix));
	}
	
	@After
	public void restoreStreams() {
		System.setOut(originalOut);
	}
}
