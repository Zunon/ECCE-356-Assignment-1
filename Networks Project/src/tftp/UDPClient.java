package tftp;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class UDPClient extends TFTPHost implements AutoCloseable {
	final DatagramSocket clientSocket = new DatagramSocket(); // The socket to connect to the server.
	InetAddress clientAddress = InetAddress.getLocalHost(); // The address of the client.
	InetAddress serverAddress = null; // The address of the server.
	String serverName = null; // The name of the server.
	Scanner stdin = new Scanner(System.in);

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
			} catch (IllegalArgumentException error) {
				System.err.println("Error while connecting: " + error.getMessage());
			}
		}
	}

	@Override
	public void close() {
		clientSocket.close();
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

	public void send(String message) {
		send(message, serverAddress, PORT, clientSocket);
	}

	public String receive() {
		return (new String(receive(clientSocket).getData()).trim());
	}

	TFTPMessage buildRequest() throws IllegalArgumentException {
		String type = getInput("Enter the type of transfer: ");
		String name = getInput("Enter the name of file to be transferred: ");
		return switch (type.toUpperCase()) {
			case "GET" -> new TFTPMessage(clientAddress, TFTPMessageType.GTRQ, name, 0, (byte[]) null, (short) 0);
			case "PUT" -> new TFTPMessage(clientAddress, TFTPMessageType.PTRQ, name, 0, (byte[]) null, (short) 0);
			default -> throw new IllegalArgumentException("Invalid request type");
		};
	}

	public static void main(String[] args) {
		try (UDPClient client = new UDPClient()) {
			client.buildRequest();
		} catch (SocketException e) {
			System.err.println("Socket Error: " + e.getMessage());
		} catch (UnknownHostException e) {
			System.err.println("Unknown Host Error: " + e.getMessage());
		}
	}
}



