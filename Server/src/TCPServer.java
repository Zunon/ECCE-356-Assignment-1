import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * TCPServer.java
 * This program is a server that waits for a client to connect to it.
 * The server then receives a message from the client and sends a confirmation.
 * The server also sends the size of a file to the client.
 * @author Khalifa AlMheiri <100045632@ku.ac.ae>
 */
public class TCPServer {
	public static final int PORT = 45632; // The port number to connect to.
	Socket socket = null; // The socket to connect to the client.
	final ServerSocket port = new ServerSocket(PORT); // The server socket.
	// The date formatter to be used in the confirmation message:
	public static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	Scanner netIn = null; // The input stream from the client.
	PrintWriter netOut = null; // The output stream to the client.
	final String myName = InetAddress.getLocalHost().toString(); // The name of the server.
	String clientName = null; // The name of the client.
	// Shutdown hook thread to close the server socket.
	static class CloseHook extends Thread {
		final TCPServer server; // The server to be closed.
		/**
		 * Constructor for the CloseHook class.
		 * @param server the server to be closed.
		 */
		CloseHook(TCPServer server) {
			this.server = server;
		}
		/**
		 * This method closes the server socket, it's invoked when the program exits
		 * normally or abnormally. Barring the JVM crashing or being killed, this
		 * method will always be called.
		 */
		public void run() {
			server.close();
			System.out.println("Shutting down server!");
		}
	}

	/**
	 * Constructor for the TCPServer class.
	 * It creates the server socket ready to be blocked for a connection.
	 * @throws IOException if the server socket cannot be created.
	 */
	public TCPServer() throws IOException {
		System.out.println("✉️ TCP Server starting at host: " + myName);
	}

	/**
	 * This method waits for a client to connect to the server socket.
	 * It then creates the input and output streams to communicate with the client.
	 * @throws IOException if the server socket cannot be created.
	 */
	public void newConnection() throws IOException {
		System.out.println("Waiting to be contacted by a Client...");
		try {
			socket = port.accept();
		} catch (SocketException e) { // This happens if an interrupt is sent while waiting for a client's message.
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

	/**
	 * This method closes the sockets and streams to cleanly quit.
	 */
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

	/**
	 * This method receives a message from the client and sends a confirmation.
	 */
	public void receiveAndConfirm() {
		String msg = netIn.nextLine();
		System.out.println(msg + " is received from Client " + clientName);
		netOut.println(formatter.format(new Timestamp(new Date().getTime())));
	}
	/**
	 * This method receives a file name from the client and sends the size of the file.
	 * If the file does not exist, it sends "File not found!".
	 */
	public void fileSizeComms() {
		String msg = netIn.nextLine();
		System.out.println("Client requested the size of the file " + msg + ".");
		File file = new File(msg);
		if (!file.exists()) {
			System.out.println("File not found!");
			netOut.println("File not found!");
		} else netOut.println(new File(msg).length());
	}

	/**
	 * This method is the main loop of the server.
	 * It waits for a client to connect, then receives messages from the client and sends confirmations.
	 * It also sends the size of a file to the client.
	 * @throws IOException if the server socket cannot be created.
	 * @throws NoSuchElementException if the client disconnects.
	 */
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

	/**
	 * This method is the main method of the server.
	 * It creates a new server and waits for a client to connect.
	 * @param args the command line arguments.
	 */
	public static void main(String[] args) {
		try {
			TCPServer server = new TCPServer();
			Runtime.getRuntime().addShutdownHook(new CloseHook(server));
			//noinspection InfiniteLoopStatement
			while (true)
				server.mainLoop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
