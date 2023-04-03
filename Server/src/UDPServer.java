
import java.io.*;
import java.net.*;
import java.util.Date;
import java.lang.Long;

public class UDPServer implements AutoCloseable {
	public static final int PORT = 45632; // The port number to connect to.
	final DatagramSocket serverSocket = new DatagramSocket(PORT); // The socket to connect to the client.
	DatagramPacket receivePacket = null; // The packet to receive data from the client.
	final InetAddress serverAddress = InetAddress.getLocalHost(); // The name of the server.
	InetAddress clientAddress = null; // The name of the client.
	int clientPort = 0; // The port number of the client.

	static class CloseHook extends Thread {
		final UDPServer server; // The server to be closed.
		/**
		 * Constructor for the CloseHook class.
		 * @param server the server to be closed.
		 */
		CloseHook(UDPServer server) {
			this.server = server;
		}
		@Override
		public void run() {
			System.out.println("Closing the server socket...");
			server.close();
		}
	}

	public UDPServer() throws SocketException, UnknownHostException {
		System.out.println("✉️ UDP Server starting at host: " + serverAddress.getHostName());
		System.out.println("Waiting to be contacted by a Client...");
	}

	public String receive() {
		byte[] receiveData = new byte[1024];
		receivePacket = new DatagramPacket(receiveData, receiveData.length);
		try {
			serverSocket.receive(receivePacket);
		} catch (IOException error) {
			System.err.println("I/O Error receiving packet: " + error.getMessage());
		}
		clientAddress = receivePacket.getAddress();
		clientPort = receivePacket.getPort();
		return (new String(receivePacket.getData()).trim());
	}

	void resFileSize(String fileName) {
		File file = new File(fileName);
		long fileSize = file.length();
		String fileSizeString = Long.toString(fileSize);
		send(fileSizeString);
	}

	void send(String message) {
		byte[] sendData;

		sendData = message.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
		try {
			serverSocket.send(sendPacket);
		} catch (IOException error) {
			System.err.println("I/O Error sending packet: " + error.getMessage());
		}
	}

	public void mainLoop() {
		String message = receive();
		System.out.println(message + " message is received from Client [" + clientAddress.getHostName() + "]..");
		send(new Date().toString());
		String fileName = receive();
		System.out.println("Client requested the size of the file " + fileName);
		resFileSize(fileName);
	}

	@Override
	public void close() {
		serverSocket.close();
	}

	public static void main(String[] args) {
		try (UDPServer server = new UDPServer()) {
			Runtime.getRuntime().addShutdownHook(new CloseHook(server));
			//noinspection InfiniteLoopStatement
			while (true)
				server.mainLoop();
		} catch (UnknownHostException error) {
			System.err.println("Could not find the localhost!");
		} catch (SocketException error) {
			System.err.println("Could not open a socket on port " + PORT);
		}
	}
}