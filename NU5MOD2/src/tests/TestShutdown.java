package tests;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import menuClient.ShutdownOption;
import pckg.UDPClient;

public class TestShutdown {

	ShutdownOption s;
	UDPClient c;
	
	private final PrintStream originalOut = System.out;
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	
	@Before
	public void setUp() {
		s = new ShutdownOption();
		c = new UDPClient("client", 8071);
	}
	
	@Test
	public void testPrintMessage() {
		System.setOut(new PrintStream(outContent));
		
		String out = "-- Shutting down client --";
		s.handleAction(c);
		assertTrue(outContent.toString().contains(out));
	}
	
	@After
	public void restoreStreams() {
		System.setOut(originalOut);
	}
}
