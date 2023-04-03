import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class UDPClient implements AutoCloseable {
	public static final int PORT = 45632; // The port number to connect to.
	final DatagramSocket clientSocket = new DatagramSocket(); // The socket to connect to the server.
	InetAddress clientAddress = InetAddress.getLocalHost(); // The address of the client.
	InetAddress serverAddress = null; // The address of the server.
	String serverName = null; // The name of the server.
	Scanner stdin = new Scanner(System.in);
	byte[] sendData = new byte[1024]; // The output buffer
	byte[] receiveData = new byte[1024]; // The input buffer

	public UDPClient() throws SocketException, UnknownHostException {
		System.out.println("UDP Client starting on host: " + clientAddress.getHostName() + ".");
		while (serverAddress == null) {
			serverName = getInput("Type name of UDP server: ");
			try {
				serverAddress = InetAddress.getByName(serverName);
			} catch (UnknownHostException e) {
				System.out.println("Unknown host, please try again.");
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

	public void send(String message) {
		sendData = message.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, PORT);
		try {
			clientSocket.send(sendPacket);
		} catch (IOException error) {
			System.err.println("I/O Error sending packet: " + error.getMessage());
		}
	}

	public String receive() {
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		try {
			clientSocket.receive(receivePacket);
		} catch (IOException error) {
			System.err.println("I/O Error receiving packet: " + error.getMessage());
		}
		return (new String(receivePacket.getData()).trim());
	}

	public static void main(String[] args) {
		try (UDPClient client = new UDPClient()) {
			// Send arbitrary message to server and get the timestamp
			client.send(client.getInput("Write any message: "));
			System.out.println("Message received successfully at " + client.receive());
			// Send file name to server and get the file size
			client.send(client.getInput("Enter the file name with extension to get the size: "));
			System.out.println("The File size is " + client.receive() + " Bytes.");
		} catch (SocketException e) {
			System.err.println("Socket Error: " + e.getMessage());
		} catch (UnknownHostException e) {
			System.err.println("Unknown Host Error: " + e.getMessage());
		}
	}
}



