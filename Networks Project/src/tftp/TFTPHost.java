package tftp;

import java.io.IOException;
import java.net.*;

public abstract class TFTPHost {
	public static final int PORT = 45632; // The port number to connect to.

	public DatagramPacket receive(DatagramSocket socket) {
		byte[] receiveData = new byte[1024];

		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		try {
			socket.receive(receivePacket);
		} catch (IOException error) {
			System.err.println("I/O Error receiving packet: " + error.getMessage());
		}
		return receivePacket;
	}

	public void send(String message, InetAddress address, int port, DatagramSocket socket) {
		byte[] sendData;

		sendData = message.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
		try {
			socket.send(sendPacket);
		} catch (IOException error) {
			System.err.println("I/O Error sending packet: " + error.getMessage());
		}
	}

	abstract void connect() throws IllegalArgumentException, SocketTimeoutException, SocketException;
}
