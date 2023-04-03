package tftp;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Scanner;

public class UDPClient extends TFTPHost implements AutoCloseable {
	final DatagramSocket clientSocket = new DatagramSocket(); // The socket to connect to the server.
	InetAddress clientAddress = InetAddress.getLocalHost(); // The address of the client.
	InetAddress serverAddress = null; // The address of the server.
	String serverName = null; // The name of the server.
	Scanner stdin = new Scanner(System.in);
	FileInputStream fileReader = null;

	public UDPClient() throws SocketException, UnknownHostException {
		System.out.println("UDP Client starting on host: " + clientAddress.getHostName() + ".");
		clientSocket.setSoTimeout(16000); // Set the timeout to be 16 seconds
		while (serverAddress == null) {
			serverName = getInput("Type name of UDP server: ");
			try {
				serverAddress = InetAddress.getByName(serverName);
				connect();
			} catch (UnknownHostException error) {
				System.err.println("Unknown host, please try again.");
			} catch (SocketTimeoutException error) {
				System.err.println("Server response timed out!");
				System.exit(0);
			}
		}
	}

	@Override
	public void close() throws IOException {
		clientSocket.close();
		if (fileReader != null) fileReader.close();
	}

	String getInput(String prompt) {
		System.out.print(prompt);
		return stdin.nextLine();
	}

	@Override
	void connect() throws SocketTimeoutException {
		send("SYN");
		String response = receive();
		if (!response.equals("SYNACK")) throw new IllegalArgumentException("Expected SYNACK");
		send("ACK");
	}

	public void send(String message) throws SocketTimeoutException {
		send(message, serverAddress, PORT, clientSocket);
	}

	public String receive() throws SocketTimeoutException {
		return (new String(receive(clientSocket).getData()).trim());
	}

	TFTPMessage buildRequest() throws IllegalArgumentException, IOException {
		String type = getInput("Enter the type of transfer: ");
		String name = getInput("Enter the name of file to be transferred: ");
		return switch (type.toUpperCase()) {
			case "GET" -> new TFTPMessage(clientAddress, TFTPMessageType.GTRQ, name, ++sequenceNumber, null, (short) 0);
			case "PUT" -> {
				File file = new File(name);
				short length = 0;
				if (!file.exists()) throw new IllegalArgumentException("File does not exist");
				length = (short) file.length();
				fileReader = new FileInputStream(file);
				byte[] body = fileReader.readAllBytes();
				yield new TFTPMessage(clientAddress, TFTPMessageType.PTRQ, name, ++sequenceNumber, body, length);
			}
			default -> throw new IllegalArgumentException("Invalid request type");
		};
	}

	public static void main(String[] args) {
		try (UDPClient client = new UDPClient()) {
			TFTPMessage message = client.buildRequest();
			client.send(message.toString());
			TFTPMessage response = new TFTPMessage(client.receive());
			switch (message.getMessageType()) {
				case GTRQ -> {
					File receivedFile = new File(response.getFileName());
						try (FileOutputStream fileWriter = new FileOutputStream(receivedFile)) {
							fileWriter.write(message.getBody());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						System.out.println("Received file: " + message.getFileName());
						System.out.println("File size: " + message.getBody().length + " bytes");
						byte[] timestamp = new Date().toString().trim().getBytes();
						client.send(new TFTPMessage(client.clientAddress, TFTPMessageType.RESP, message.getFileName(), message.getSequenceNumber() + 1, timestamp, (short)timestamp.length).toString());
				}
				case PTRQ -> {
					System.out.println("File sent successfully!");
					System.out.println("File received at: " + response.getBodyAsString());
				}
			}
			client.send("FIN");
			client.receive();
			client.send("ACK");
			System.out.println("Terminated successfully, exiting...");
		} catch (SocketException e) {
			System.err.println("Socket Error: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("I/O Error: " + e.getMessage());
		}
	}
}



