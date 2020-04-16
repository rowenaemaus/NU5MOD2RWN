package tests;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pckg.UDPServer;

public class TestServer {
	
	private UDPServer s;
	private final PrintStream originalOut = System.out;
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	
	@Before
	public void setUp() {
		s = new UDPServer();
	}
	
	@Test
	public void testPrintMessage() {
		System.setOut(new PrintStream(outContent));
		String print = "print";
		s.printMessage(print);
		assertTrue(outContent.toString().contains(print));
	}

	@After
	public void restoreStreams() {
		System.setOut(originalOut);
	}
}
