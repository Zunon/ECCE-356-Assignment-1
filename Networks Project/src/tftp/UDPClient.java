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
		clientSocket.setSoTimeout(60000); // Set the timeout to be 60 seconds
		while (serverAddress == null) {
			serverName = getInput("Type name of UDP server: ");
			try {
				serverAddress = InetAddress.getByName(serverName);
				connect();
			} catch (UnknownHostException error) {
				System.err.println("Unknown host, please try again.");
			} catch (SocketTimeoutException error) {
				System.err.println("Server response timed out!");
				System.err.println(error.getMessage());
				System.exit(0);
			} catch (IOException e) {
				System.err.println(e.getMessage());
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
		String line = stdin.nextLine();
		if (line.equals("quit")) return null;
		return line;
	}

	@Override
	void connect() throws SocketTimeoutException, IOException {
		clientSocket.setSoTimeout(200); // Set the timeout to be 3 seconds for acknowledgements
		clientSocket.send(new DatagramPacket("SYN".getBytes(), 3, serverAddress, TFTPHost.PORT));
		byte[] receiveData = new byte[4096];
		DatagramPacket responseData = new DatagramPacket(receiveData, receiveData.length);
		for (int i = 1; i <= 3 && responseData.getAddress() == null; i++)
			try {
					clientSocket.receive(responseData);
			} catch (SocketTimeoutException error) {}
		if (responseData.getAddress() == null) throw new SocketTimeoutException("Connection failed!");
		String response = new String(responseData.getData()).trim();
		if (!response.equals("SYNACK")) throw new IllegalArgumentException("Expected SYNACK");
		clientSocket.send(new DatagramPacket("ACK".getBytes(), 3, serverAddress, TFTPHost.PORT));
		clientSocket.setSoTimeout(60000);
	}

	public void send(String message) throws SocketTimeoutException, SocketException {
		clientSocket.setSoTimeout(200); // Set the timeout to be 3 seconds for acknowledgements
		send(message, serverAddress, PORT, clientSocket);
		clientSocket.setSoTimeout(60000);
	}

	public TFTPMessage receive() throws SocketTimeoutException {
		return receive(clientSocket);
	}

	TFTPMessage buildRequest() throws IllegalArgumentException, IOException {
		String type = getInput("Enter the type of transfer: ");
		if (type == null) return null;
		String name = getInput("Enter the name of file to be transferred: ");
		if (name == null) return null;
		return switch (type.toUpperCase()) {
			case "GET" -> new TFTPMessage(clientAddress, TFTPMessageType.GTRQ, name, ++sequenceNumber, null, (short) 0);
			case "PUT" -> {
				File file = new File(name);
				if (!file.exists()) throw new IllegalArgumentException("File does not exist");
				fileReader = new FileInputStream(file);
				byte[] body = fileReader.readNBytes((int) Math.min(file.length(), 1024));
				yield new TFTPMessage(clientAddress, TFTPMessageType.PTRQ, name, ++sequenceNumber, body, (short) Math.min(file.length(), 1024));
			}
			default -> throw new IllegalArgumentException("Invalid request type");
		};
	}

	public void sendFile(TFTPMessage message) throws IOException {
		File file = new File(message.getFileName());
		long remainder = Math.max(1024, file.length()) - 1024;
		System.out.println("Sending file: " + message.getFileName());
		System.out.println("File size: " + file.length() + " bytes");
		while (remainder >= 0) {
			byte[] body = fileReader.readNBytes((int)Math.min(remainder, 1024));
			TFTPMessage newMessage = new TFTPMessage(clientAddress, TFTPMessageType.DATA, message.getFileName(), ++sequenceNumber, body, (short) Math.min(remainder, 1024));
			send(newMessage.toString());
			if (remainder == 0) break;
			remainder = Math.max(1024, remainder) - 1024;
		}
	}

	public static void main(String[] args) {
		try (UDPClient client = new UDPClient()) {
			boolean done = false;
			while (!done) {
				TFTPMessage message = client.buildRequest();
				if (message == null) {
					done = true;
					continue;
				}
				client.send(message.toString());
				if (message.getMessageType() == TFTPMessageType.PTRQ) {
					client.sendFile(message);
				}
				TFTPMessage response = client.receive();
				switch (message.getMessageType()) {
					case GTRQ -> {
						if (response.getMessageType() == TFTPMessageType.RESP && response.getBodyAsString().equals("File not found!")) {
							System.out.println("File not found!");
							continue;
						}
						File receivedFile = new File(response.getFileName());
						if (receivedFile.exists()) {
							receivedFile = new File(response.getFileName() + ".client_received");
						}
						long totalBytes = response.getLength();
						try (FileOutputStream fileWriter = new FileOutputStream(receivedFile)) {
							ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
							outputStream.write(response.getBody());
							response = client.receive();
							while (response.getMessageType() == TFTPMessageType.DATA && response.getLength() > 0) {
								outputStream.write(response.getBody());
								totalBytes += response.getLength();
								response = client.receive();
							}
							fileWriter.write(outputStream.toByteArray());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						System.out.println("Received file: " + response.getFileName());
						System.out.println("File size: " + totalBytes + " bytes");
						byte[] timestamp = new Date().toString().trim().getBytes();
						client.send(new TFTPMessage(client.clientAddress, TFTPMessageType.RESP, response.getFileName(), ++client.sequenceNumber, timestamp, (short) timestamp.length).toString());
					}
					case PTRQ -> {
						System.out.println("File sent successfully!");
						System.out.println("File received at: " + response.getBodyAsString());
					}
				}
			}
			client.send(new TFTPMessage(client.clientAddress, TFTPMessageType.RESP, "DISCONNECTING", ++client.sequenceNumber, "FIN".getBytes(), (short) 3).toString());
			client.receive();
			client.send(new TFTPMessage(client.clientAddress, TFTPMessageType.RESP, "DISCONNECTING", ++client.sequenceNumber, "KCA".getBytes(), (short) 3).toString());
			System.out.println("Terminated successfully, exiting...");
		} catch (SocketException e) {
			System.err.println("Socket Error: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("I/O Error: " + e.getMessage());
		}
	}
}



