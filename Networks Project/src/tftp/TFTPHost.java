package tftp;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

public abstract class TFTPHost {
	public static final int PORT = 45632; // The port number to connect to.
	int sequenceNumber = 0; // The sequence number for the connection.
	public final InetAddress currentHostAddress = InetAddress.getLocalHost();
	private final int retry_max = 600000;
	private int retries = retry_max;
	private Queue<TFTPMessage> packetQueue = new LinkedList<>();

	protected TFTPHost() throws UnknownHostException {
	}

	TFTPMessage dequeue(DatagramSocket socket) throws IOException {
		DatagramPacket packet = new DatagramPacket(new byte[4096], 4096);
		if (packetQueue.isEmpty()) {
			socket.receive(packet);
			return new TFTPMessage(packet);
		} else return packetQueue.remove();
	}

	public TFTPMessage receive(DatagramSocket socket) throws SocketTimeoutException {
		byte[] receiveData = new byte[4096];
		TFTPMessage message = null;
		try {
			message = dequeue(socket);
		} catch (SocketTimeoutException error) {
			throw new SocketTimeoutException("Timed out receiving packet: " + error.getMessage());
		} catch (IOException error) {
			System.err.println("I/O Error receiving packet: " + error.getMessage());
		}
		if (message == null) throw new SocketTimeoutException("Timed out waiting for packet.");
		if (!message.getBodyAsString().equals("ACK"))
			System.out.println("Packet Received: " + message.pretty());
		if (!message.getBodyAsString().equals("ACK") && message.getSequenceNumber() == sequenceNumber + 1) {
			TFTPMessage acknowledgement = new TFTPMessage(currentHostAddress, TFTPMessageType.RESP, message.getFileName(), message.sequenceNumber, "ACK".getBytes(), (short)3);
			send(acknowledgement.toString(), message.getHostAddress(), message.port, socket);
			System.out.println("Acknowledged packet " + message.sequenceNumber);
			sequenceNumber = message.sequenceNumber;
			return message;
		}
		if (message.sequenceNumber <= sequenceNumber && !message.getBodyAsString().trim().equals("ACK")) {
			System.out.println("Received packet " + message.sequenceNumber + " but expected " + (sequenceNumber) + ", discarding.");
			TFTPMessage acknowledgement = new TFTPMessage(currentHostAddress, TFTPMessageType.RESP, message.getFileName(), message.sequenceNumber, "ACK".getBytes(), (short)3);
			send(acknowledgement.toString(), message.getHostAddress(), message.port, socket);
			return receive(socket);
		}
		return message;
	}

	public void send(String message, InetAddress address, int port, DatagramSocket socket) throws SocketTimeoutException {
		byte[] sendData;
		sendData = message.getBytes();
		if (!new TFTPMessage(message).getBodyAsString().equals("ACK"))
			System.out.println("Sending packet: " + new TFTPMessage(message).pretty());
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
		try {
//			if (Math.random() > 0.001) // Randomly don't send
				socket.send(sendPacket);
			sequenceNumber = new TFTPMessage(message).sequenceNumber;
			if (!(new TFTPMessage(message).getBodyAsString().trim().equals("ACK"))) {
				TFTPMessage response = receive(socket);
				if (!response.getBodyAsString().equals("ACK") || response.getSequenceNumber() != sequenceNumber) {
					System.err.println("Error: Expected ACK " + sequenceNumber + " but received " + response.getBodyAsString() + " (" + response.getSequenceNumber() + ")");
					if (response.getSequenceNumber() > sequenceNumber)
						packetQueue.add(response);
					send(sendPacket, socket);
				} else
					System.out.println("Received ACK " + response.getSequenceNumber());
			}
		} catch (SocketTimeoutException error) {
			System.err.println("Timed out sending packet: " + error.getMessage());
			send(sendPacket, socket);
			return;
		} catch (IOException error) {
			System.err.println("I/O Error sending packet: " + error.getMessage());
		}
	}

	public void send(DatagramPacket timedOut, DatagramSocket socket) {
		if (retries == 0) {
			System.err.println("Error: Maximum number of retries reached.");
			return;
		}
		if (sequenceNumber > new TFTPMessage(timedOut).sequenceNumber) {
			return;
		}
		System.out.println("Resending packet...");
		try {
			retries--;
			System.out.println(retries + " retries remaining.");
			socket.send(timedOut);
			TFTPMessage response = receive(socket);
			if (!response.getBodyAsString().equals("ACK") || response.getSequenceNumber() != sequenceNumber) {
				System.err.println("Error: Expected ACK " + sequenceNumber + " but received " + response.getBodyAsString() + " (" + response.getSequenceNumber() + ")");
				if (response.getSequenceNumber() == sequenceNumber + 1)
					packetQueue.add(response);
				if (response.getBodyAsString().equals("ACK") && response.sequenceNumber > sequenceNumber) sequenceNumber = response.sequenceNumber;
				else throw new SocketTimeoutException("Timed out waiting for ACK " + sequenceNumber);
			}
			System.out.println("Received ACK " + response.getSequenceNumber());
			retries = retry_max;
		} catch (SocketTimeoutException error) {
			send(timedOut, socket);
		} catch (IOException error) {
			System.err.println("I/O Error sending packet: " + error.getMessage());
		}
	}

	abstract void connect() throws IllegalArgumentException, SocketTimeoutException, SocketException, IOException;
}
