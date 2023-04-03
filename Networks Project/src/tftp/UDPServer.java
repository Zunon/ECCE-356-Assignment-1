package tftp;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.lang.Long;
import java.util.Objects;

public class UDPServer extends TFTPHost implements AutoCloseable {
	final DatagramSocket serverSocket = new DatagramSocket(PORT); // The socket to connect to the client.
	final InetAddress serverAddress = InetAddress.getLocalHost(); // The name of the server.
	InetAddress clientAddress = null; // The name of the client.
	int clientPort = 0; // The port number of the client.

	@Override
	void connect() throws IllegalArgumentException, SocketException, SocketTimeoutException {
		String response = receive();
		if (!response.equals("SYN")) throw new IllegalArgumentException("Expected SYN!");
		serverSocket.setSoTimeout(16000);
		send("SYNACK");
		response = receive();
		if (!response.equals("ACK")) throw new IllegalArgumentException("Expected ACK!");
	}

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
		DatagramPacket receipt = receive(serverSocket);
		clientAddress = receipt.getAddress();
		clientPort = receipt.getPort();
		return (new String(receipt.getData()).trim());
	}

	void send(String message) {
		send(message, clientAddress, clientPort, serverSocket);
	}

	public void mainLoop() {
		// 3-way handshake
		try {
			connect();
		} catch(SocketException | IllegalArgumentException | SocketTimeoutException error) {
			System.out.println("Connection Failed!");
			return;
		}
		// receive respond loop
		// terminate on timeout or fin handshake
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