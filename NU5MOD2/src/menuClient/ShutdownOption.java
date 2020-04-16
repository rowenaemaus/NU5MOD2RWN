package menuClient;

import pckg.UDPClient;

public class ShutdownOption implements MenuOptionInterface{
	@Override
	public void handleAction(UDPClient c) {
		c.printMessage("-- Shutting down client --");
		c.getUdp().closeConnection();
	}
}
