package MenuClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import pckg.UDPClient;
import udp.UDProtocol;

public class RequestOption implements MenuOptionInterface{

	private UDPClient c;
	private UDProtocol p;
	private List<String> fileRequest = new ArrayList<String>();
	private Thread download;
	private Thread pause;

	@Override
	public void handleAction(UDPClient c) {
		this.c = c;
		this.p = c.getUdp();
		c.printMessage("-- Requesting file from server --");

		fileRequest.clear();
		askFilesToGet();

		if (fileRequest.isEmpty()) { 
			c.printMessage(">> Nothing to receive. Cancelling file request.");
			return;
		}

		for (String s : fileRequest) {
			p.gimmeFile(s);
		}
		
		
//		download = new Thread(new Download());
//		pause = new Thread(new Pause());
//
//		download.start();
//		pause.start();
	}

	public void askFilesToGet() {
		c.printMessage(">> What file(s) would you like to request? (Please enter with spaces in between)\n...");
		String answer = c.getAnswer();

		String[] answers = answer.split(" ");

		for (String s : answers) {
			if (!p.getFileListString().contains(answer) && !answer.contains(".")) {
				c.printMessage(String.format(">> WARNING: invalid file '%s', not requesting...", s));
			} else {
				c.printMessage(String.format(">> Adding %s to request from server...", s));
				fileRequest.add(s);
			}
		}

		c.printMessage(String.format(">> You are about to request: %s. Do you want to proceed (yes/no)?", fileRequest.toString()));
		answer = c.getAnswer();

		while (!(answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("no"))) {
			c.printMessage(">> WARNING: Invalid response, please try again\n...");
			answer = c.getAnswer();
		}

		switch (answer) {
		case "yes":
			return;
		case "no:":
			fileRequest.clear();
			return;
		}
	}	

	public class Download implements Runnable{

		public Download() {
		}

		@Override
		public void run() {
			
		}
	}

	public class Pause implements Runnable{

		public Scanner keyboard = new Scanner(System.in);
		public String pause = "p";
		public String typed = "";

		@SuppressWarnings("static-access")
		@Override
		public void run() {
			while (download.isAlive()) {
				try {
					Thread.sleep(TimeUnit.SECONDS.toMillis(100));
					download.sleep(TimeUnit.SECONDS.toMillis(10000));
					System.out.println("................");
					System.out.println("................");
					System.out.println("GET INPUT");
					System.out.println("................");
					System.out.println("................");
					typed = keyboard.next();
					if (pause.equalsIgnoreCase(pause)) {
						System.out.println("................");
						System.out.println("................");
						System.out.println("PAUSING DOWNLOAD");
						System.out.println("................");
						System.out.println("................");
						download.wait();
					} else {
						System.out.println("................");
						System.out.println("................");
						System.out.println("RESUMING DOWNLOAD");
						System.out.println("................");
						System.out.println("................");
						download.notify();
					}
				} catch (Exception e) { 
					// e
				}
			}
		}

		//		@Override
		//		public void keyTyped(KeyEvent e) {
		//			int key = e.getKeyCode();
		//
		//			if (key == KeyEvent.VK_SPACE) {
		//				try {
		//					download.wait();
		//				} catch (InterruptedException e1) {
		//					c.printMessage(">> ERROR: something went wrong with pausing the download");
		//					e1.printStackTrace();
		//				}	
		//			} else {
		//				download.notify();	
		//			}
		//		}
		//
		//		@Override
		//		public void keyPressed(KeyEvent e) {
		//			int key = e.getKeyCode();
		//
		//			if (key == KeyEvent.VK_SPACE) {
		//				try {
		//					download.wait();
		//				} catch (InterruptedException e1) {
		//					c.printMessage(">> ERROR: something went wrong with pausing the download");
		//					e1.printStackTrace();
		//				}	
		//			} else {
		//				download.notify();	
		//			}
		//
		//		}
		//
		//		@Override
		//		public void keyReleased(KeyEvent e) {
		//			// TODO Auto-generated method stub
		//
		//		}
	}

}

