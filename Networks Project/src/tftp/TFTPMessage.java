package tftp;

import java.net.InetAddress;
// Note: TFTP st
enum TFTPMessageType { GTRQ, PTRQ, DATA, RESP }

public class TFTPMessage {
	InetAddress hostAddress;
	TFTPMessageType messageType;
	String fileName;
	int sequenceNumber;
	byte[] body;
	short length;

	public TFTPMessage(InetAddress hostAddress, TFTPMessageType messageType, String fileName, int sequenceNumber, byte[] body, short length) {
		this.hostAddress = hostAddress;
		this.messageType = messageType;
		this.body = body;
		this.length = length;
		this.fileName = fileName;
		this.sequenceNumber = sequenceNumber;
	}

	public TFTPMessage(String message) {
		String[] messageParts = message.split("\r\n");
		try {
			hostAddress = InetAddress.getByName(messageParts[0]);
			messageType = TFTPMessageType.valueOf(messageParts[1]);
			fileName = messageParts[2];
			sequenceNumber = Integer.parseInt(messageParts[3]);
			length = Short.parseShort(messageParts[4]);
			body = messageParts[5].getBytes();
		} catch (Exception e) {
			System.out.println("Error parsing message: " + e.getMessage());
		}
	}

	public InetAddress getHostAddress() {
		return hostAddress;
	}

	public TFTPMessageType getMessageType() {
		return messageType;
	}

	public byte[] getBody() {
		return body;
	}

	public short getLength() {
		return length;
	}

	public String toString() {
		return hostAddress.toString() + "\r\n" + messageType.toString() + "\r\n"
						+ fileName + "\r\n" + sequenceNumber + "\r\n"
						+ length + "\r\n\r\n"
						+ body.toString();
	}
}
