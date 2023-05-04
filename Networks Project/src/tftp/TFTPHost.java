package tftp;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public abstract class TFTPHost {
	public static final int PORT = 45632; // The port number to connect to.
	int sequenceNumber = 0; // The sequence number for the connection.
	public final InetAddress currentHostAddress = InetAddress.getLocalHost();

	protected TFTPHost() throws UnknownHostException {
	}

	public DatagramPacket receive(DatagramSocket socket) throws SocketTimeoutException {
		byte[] receiveData = new byte[4096];

		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		try {
			socket.receive(receivePacket);
		} catch (IOException error) {
			System.err.println("I/currentHostAddressO Error receiving packet: " + error.getMessage());
		}
		if (receivePacket.getAddress() == null) throw new SocketTimeoutException("Timed out waiting for packet.");
		System.out.println("Packet Received: " + new TFTPMessage(new String(receivePacket.getData()).trim()).pretty());
		TFTPMessage response = new TFTPMessage(new String(receivePacket.getData()).trim());
		if (!response.getBodyAsString().equals("ACK")) {
			TFTPMessage acknowledgement = new TFTPMessage(currentHostAddress, TFTPMessageType.RESP, response.getFileName(), response.sequenceNumber, "ACK".getBytes(), (short)3);
			send(acknowledgement.toString(), receivePacket.getAddress(), receivePacket.getPort(), socket);
			System.out.println("Acknowledged packet " + response.sequenceNumber);
			sequenceNumber = response.sequenceNumber;
		}
		return receivePacket;
	}

	public void send(String message, InetAddress address, int port, DatagramSocket socket) throws SocketTimeoutException {
		byte[] sendData;

		sendData = message.getBytes();
		System.out.println("Sending packet: " + new TFTPMessage(message).pretty());
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
		try {
			socket.send(sendPacket);
			sequenceNumber = new TFTPMessage(message).sequenceNumber;
			if (!(new TFTPMessage(message).getBodyAsString().trim().equals("ACK"))) {
				String msg = new String(receive(socket).getData()).trim();
				TFTPMessage response = new TFTPMessage(msg);
				if (!response.getBodyAsString().equals("ACK") || response.getSequenceNumber() != sequenceNumber) {
					System.err.println("Error: Expected ACK " + sequenceNumber + " but received " + response.getBodyAsString() + " (" + response.getSequenceNumber() + ")");
				}
				System.out.println("Received ACK " + response.getSequenceNumber());
			}
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
			return;
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

	abstract void connect() throws IllegalArgumentException, SocketTimeoutException, SocketException, IOException;
}
