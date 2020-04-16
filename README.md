# NU5MOD2RWN
NU5 Module 2 Rowena Emaus

-----------------

Main classes:
- UDProtocol      : User Datagram Protocol for file transfer
- UDPServer       : Server class to run server
- UDPClient       : Client class to run client
---------------- 

__*class UDProtocol*__  
Main class containing all the functionality for sending and receiving DatagramPackets. In package 'udp'.
To instantiate a new UDProtocol this is the format: _new UDProtocol(String name, int port, File fileLocation)_

__*class UDPServer*__  
Main class to run a server using UDP. The class extends Runnable, and listens for packets until it is shut down. The class can be
found in package 'pckg'. For running it, you can provide an argument String for the filelocation. Default filelocation is
"/home/pi/udp2" when no argument is provided.

__*class UDPClient*__  
Main class to run a client using UDP. The class extends Runnable and can be found in package 'pckg'. Running a new Client, you 
can provide an argument for the filelocation on your system. Default filelocation is _System.getProperty("user.home")+"/Downloads/udp"_ when
no argument is provided.

-----------------
__*Packages*__
1. filesRowena : Holds all files to potentially be transferred.
2. menuClient : Holds all classes that each handle a different option of the client menu
3. pckg : Holds all basic important classes (UDPServer, UDPClient)
4. test : Holds all tests
5. udp : Holds the UDProtocol and all implementation classes of PacketHandler

-----------------

__*package menuClient*__  
This package holds classes that are all implementations of the MenuOptionInterface. The UDPClient has a menu where it selects options
for the user to perform certain actions. Every menu option has its own MenuOptionInterface subclass to deal with this.
In UDPClient the options are defined in an Enum, also linking each option to the right class.

-----------------

__*package udp*__  
This package first of all holds the UDProtocol. This is the main protocol class holding most of the basic logic. However, 
different types of packets can be transferred using the protocol. In the UDProtocol , these different packet types are defined
in an enum. Each level of this enum has a specific subclass implementation of the PacketHandler interface attached. These classes
each handle the packet differently. These PacketHandlers are also in this package.

==================  
*NOTE* : Server should be running before starting client!

__To start server and client from IDE__  
run 'UDPServer' first, then run 'UDPClient'  

__To start the server on the Raspberry Pi__  
run 'java -jar UDPRowenaSrv.jar [args]' (optional filelocation arg)  

__To start the client from terminal__  
run 'java -jar UDPRowenaCli.jar [args]' (optional filelocation arg)
