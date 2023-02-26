import java.net.*;
import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class TCPServer {
	public static final int PORT = 45632;
	Socket socket = null;
	ServerSocket port = new ServerSocket(PORT);
	public static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	Scanner netIn = null;
	PrintWriter netOut = null;
	String myName = InetAddress.getLocalHost().getHostAddress();
	String clientName = null;

	static class CloseHook extends Thread {
		TCPServer server;

		CloseHook(TCPServer server) {
			this.server = server;
		}

		public void run() {
			server.close();
			System.out.println("Shutting down server!");
		}
	}
	public TCPServer() throws IOException {
		System.out.println("✉️ TCP Server starting at host: " + myName);
	}

	public void newConnection() throws IOException {
		System.out.println("Waiting to be contacted by a Client...");
		try {
			socket = port.accept();
		} catch (SocketException e) {
			System.out.println("Server closed while waiting for message!");
			return;
		}
		System.out.println("A connection is established with a Client");
		DataInputStream inStream = new DataInputStream(socket.getInputStream());
		DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
		netIn = new Scanner(new InputStreamReader(inStream));
		netOut = new PrintWriter(outStream, true);
		clientName = socket.getInetAddress().toString();
	}

	public void close() {
		try {
			port.close();
			if (socket != null) {
				socket.close();
				netIn.close();
				netOut.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void receiveAndConfirm() {
		String msg = netIn.nextLine();
		System.out.println(msg + " is received from Client " + clientName);
		netOut.println(formatter.format(new Timestamp(new Date().getTime())));
	}

	public void fileSizeComms() {
		String msg = netIn.nextLine();
		System.out.println("Client requested the size of the file " + msg + ".");
		File file = new File(msg);
		if (!file.exists()) {
			System.out.println("File not found!");
			netOut.println("File not found!");
		} else netOut.println(new File(msg).length());
	}

	public void mainLoop() throws IOException {
		newConnection();
		try {
			while (netIn.hasNextLine()) {
				receiveAndConfirm();
				fileSizeComms();
			}
		} catch (NoSuchElementException e) {
			System.out.println("Client disconnected.");
			netIn.close();
			netIn = null;
			netOut.close();
			netOut = null;
			socket.close();
			socket = null;
			clientName = null;
		}
	}

	public static void main(String[] args) {
		try {
			TCPServer server = new TCPServer();
			Runtime.getRuntime().addShutdownHook(new CloseHook(server));
			while (true)
				server.mainLoop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
