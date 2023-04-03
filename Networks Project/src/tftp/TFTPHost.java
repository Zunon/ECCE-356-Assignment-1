package tftp;

import java.io.IOException;
import java.net.*;

public abstract class TFTPHost {
	public static final int PORT = 45632; // The port number to connect to.
	int sequenceNumber = 0; // The sequence number for the connection.

	public DatagramPacket receive(DatagramSocket socket) throws SocketTimeoutException {
		byte[] receiveData = new byte[1024];

		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		try {
			socket.receive(receivePacket);
		} catch (IOException error) {
			System.err.println("I/O Error receiving packet: " + error.getMessage());
		}
		if (receivePacket.getAddress() == null) throw new SocketTimeoutException("Timed out waiting for packet.");
		return receivePacket;
	}

	public void send(String message, InetAddress address, int port, DatagramSocket socket) throws SocketTimeoutException {
		byte[] sendData;

		sendData = message.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
		try {
			socket.send(sendPacket);
		} catch (SocketTimeoutException error) {
			System.err.println("Timed out sending packet: " + error.getMessage());
			System.out.println("Resending packet...");
			send(sendPacket, socket);
			String msg = receive(socket).getData().toString();
			if (msg.split("\r\n").length >= 6) {
				TFTPMessage response = new TFTPMessage(msg);
				if (!response.getBodyAsString().equals("ACK") || response.getSequenceNumber() != sequenceNumber) {
					System.err.println("Error: Expected ACK " + sequenceNumber + " but received " + response.getBodyAsString());
				}
			}
		} catch (IOException error) {
			System.err.println("I/O Error sending packet: " + error.getMessage());
		}
	}

	public void send(DatagramPacket timedOut, DatagramSocket socket) {
		try {
			socket.send(timedOut);
		} catch (SocketTimeoutException error) {
			send(timedOut, socket);
		} catch (IOException error) {
			System.err.println("I/O Error sending packet: " + error.getMessage());
		}
	}

	abstract void connect() throws IllegalArgumentException, SocketTimeoutException, SocketException;
}
