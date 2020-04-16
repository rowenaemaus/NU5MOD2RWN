package MenuClient;

import pckg.UDPClient;

public class ContentOption implements MenuOptionInterface {

	@Override
	public void handleAction(UDPClient c) {
		c.printMessage("-- Requesting contents of server --");
		c.getUdp().sendContentRequest();
	}
}
