package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import pckg.UDPClient;
import pckg.UDPClient.Menu;
import udp.UDProtocol;

public class TestClient {

	private static UDPClient c;
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private ByteArrayInputStream inContent;
	private final PrintStream originalOut = System.out;
	private InputStream originalIn = System.in; 
	

	@Before
	public void setUpStream() {
//		System.setOut(new PrintStream(outContent));
	}

	@BeforeAll
	public static void setUp() {
		c = new UDPClient("client", 8071);
	}

	@Test
	public void testPrintMenu() {
		System.setOut(new PrintStream(outContent));
		c.printMenu();		
		assertTrue(outContent.toString().contains(Menu.CONTENT.menuText));
		assertTrue(outContent.toString().contains(Menu.REQUEST.menuText));
		assertTrue(outContent.toString().contains(Menu.SEND.menuText));
		assertTrue(outContent.toString().contains(Menu.DELETE.menuText));
		assertTrue(outContent.toString().contains(Menu.QUIT.menuText));
	}
	
	@Test
	public void testCheckValidOption() {
		assertEquals(c.checkValidOption(Menu.CONTENT.option),Menu.CONTENT);
		assertEquals(c.checkValidOption(Menu.REQUEST.option),Menu.REQUEST);
		assertEquals(c.checkValidOption(Menu.SEND.option),Menu.SEND);
		assertEquals(c.checkValidOption(Menu.DELETE.option),Menu.DELETE);
		assertEquals(c.checkValidOption(Menu.QUIT.option),Menu.QUIT);
	}
	
//	@Test
	// inputstream mocken lukt niet, andere uni deelnemers ook geen ideeen.
	public void testGetAnswer() {
		System.setOut(new PrintStream(outContent, false)); // set flush to false?
		String mockedUserInput = "this is my answer\n";
		inContent = new ByteArrayInputStream(mockedUserInput.getBytes());
		System.setIn(inContent);
		System.out.println("flag1");

		String answer = c.getAnswer();
		System.out.println("flag2");
		assertEquals(answer, mockedUserInput);
		System.out.println("flag3");

		System.setIn(originalIn);	
	}

	@After
	public void restoreStreams() {
		System.setOut(originalOut);
		System.setIn(originalIn);
	}
}