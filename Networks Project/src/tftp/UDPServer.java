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
	FileInputStream fileReader = null; // The file reader for the file to be sent.

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
			try {
				server.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public UDPServer() throws SocketException, UnknownHostException {
		System.out.println("✉️ UDP Server starting at host: " + serverAddress.getHostName());
		System.out.println("Waiting to be contacted by a Client...");
	}

	public String receive() throws SocketTimeoutException {
		DatagramPacket receipt = receive(serverSocket);
		clientAddress = receipt.getAddress();
		clientPort = receipt.getPort();
		return (new String(receipt.getData()).trim());
	}

	void send(String message) throws SocketTimeoutException {
		send(message, clientAddress, clientPort, serverSocket);
	}

	public void mainLoop() throws IOException {
		// 3-way handshake
		try {
			connect();
		} catch(SocketException | IllegalArgumentException | SocketTimeoutException error) {
			System.out.println("Connection Failed!");
			serverSocket.setSoTimeout(0);
			System.out.println("Waiting to be contacted by a Client...");
			return;
		}
		// receive respond loop
		boolean done = false;
		try {
			while (!done) {
				String request = receive();
				if (request.equals("FIN")) {
					done = true;
					continue;
				}
				TFTPMessage message = new TFTPMessage(request);
				sequenceNumber = message.getSequenceNumber();
				switch (message.getMessageType()) {
					case PTRQ -> {
						File receivedFile = new File(message.getFileName());
						try (FileOutputStream fileWriter = new FileOutputStream(receivedFile)) {
							fileWriter.write(message.getBody());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						System.out.println("Received file: " + message.getFileName());
						System.out.println("File size: " + message.getBody().length + " bytes");
						byte[] timestamp = new Date().toString().trim().getBytes();
						send(new TFTPMessage(serverAddress, TFTPMessageType.RESP, message.getFileName(), message.getSequenceNumber() + 1, timestamp, (short)timestamp.length).toString());
					}
					case GTRQ -> {
						File requestedFile = new File(message.getFileName());
						if (!requestedFile.exists()) {
							System.err.println("File not found: " + message.getFileName());
							return;
						}
						fileReader = new FileInputStream(requestedFile);
						short length = (short) requestedFile.length();
						byte[] fileBytes = fileReader.readAllBytes();
						TFTPMessage response = new TFTPMessage(serverAddress, TFTPMessageType.RESP, message.getFileName(), ++sequenceNumber, fileBytes, length);
						send(response.toString());
						System.out.println("Sent file: " + message.getFileName());
						System.out.println("File size: " + length + " bytes");
						System.out.println("Received at: " + new TFTPMessage(receive()).getBodyAsString());
					}
					default -> throw new IllegalArgumentException("Invalid message type!");
				}
			}
		} catch (SocketTimeoutException error) {
			System.out.println("Connection timed out!");
		}
		// terminate on timeout or fin handshake
		if (done) {
			send("FINACK");
			receive();
		}
		serverSocket.setSoTimeout(0);
		System.out.println("Waiting to be contacted by a Client...");
	}

	@Override
	public void close() throws IOException {
		serverSocket.close();
		if (fileReader != null) fileReader.close();
	}

	public static void main(String[] args) {
		try (UDPServer server = new UDPServer()) {
			Runtime.getRuntime().addShutdownHook(new CloseHook(server));
			//noinspection InfiniteLoopStatement
			while (true)
				server.mainLoop();
		} catch (UnknownHostException error) {
			System.err.println("Could not find the localhost!");
		} catch (IOException error) {
			System.err.println("Could not open a socket on port " + PORT);
		}
	}
}