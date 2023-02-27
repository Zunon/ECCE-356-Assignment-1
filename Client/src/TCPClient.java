import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * TCPClient.java
 * This program is a client that connects to a server and sends a message to it.
 * The client also inquires the server for the size of a file.
 * @author Khalifa AlMheiri <100045632@ku.ac.ae>
 */
public class TCPClient {
	public static final int PORT = 45632; // The port number to connect to.
	final Scanner stdIn = new Scanner(System.in); // The standard input stream.
	Scanner netIn; // The input stream from the server.
	PrintWriter netOut; // The output stream to the server.
	Socket socket; // The socket to connect to the server.
	final String myName = InetAddress.getLocalHost().toString(); // The name of the client.
	/**
	 * Constructor for the TCPClient class.
	 * It requests the server name from the user and connects to it.
	 * after that it creates the input and output streams.
	 * @throws IOException if the connection fails.
	 * @throws UnknownHostException if the host name is not found.
	 * @throws ConnectException if the server is not running.
	 */
	TCPClient() throws IOException {
		System.out.println("TCP Client starting on host: " + myName);
		String serverName = prompt("Type name of TCP Server: ");
		socket = new Socket(serverName, PORT);
		DataInputStream inStream = new DataInputStream(socket.getInputStream());
		DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
		netIn = new Scanner(new InputStreamReader(inStream));
		netOut = new PrintWriter(outStream, true);
	}
	/**
	 * This method sends a message to the server and receives a response from it.
	 * @param prompt the message to be sent to the server.
	 * @return the response from the server.
	 */
	public String sendAndReceive(String prompt) {
		String message = prompt(prompt);
		netOut.println(message);
		String response = null;
		try {
			response = netIn.nextLine();
		} catch (NoSuchElementException e) {
			System.out.println("Server has closed the connection!");
			close();
		}
		return (response);
	}
	/**
	 * This method prompts the user for a message and returns it.
	 * If the user enters "quit" the program will exit.
	 * @param message the message to be displayed to the user.
	 * @return the message entered by the user.
	 */
	public String prompt(String message) {
		System.out.print(message);
		String str = stdIn.nextLine();
		if (str.equals("quit"))
			close();
		return (str);
	}
	/**
	 * This method is the main loop of the client.
	 * It sends a message to the server and receives a response from it.
	 * Then it asks the user for a file name and sends it to the server.
	 * The server then sends the size of the file back to the client.
	 */
	public void mainLoop() {
		String response = sendAndReceive( "Write any message: ");
		System.out.println("Message received successfully at " + response);
		response = sendAndReceive("Enter the file name with extension to get the size: ");
		if (response.equals("File not found!"))
			System.out.println("File not found!");
		else
			System.out.println("The File size is: " + response + " Bytes.");
	}
	/**
	 * This method closes the connection and exits the program.
	 */
	public void close() {
		System.out.println("Shutting down client...");
		try {
			if (socket != null) {
				socket.close();
				netIn.close();
				netOut.close();
			}
			stdIn.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	/**
	 * This is the main method of the program.
	 * It creates an instance of the TCPClient class and calls its mainLoop method.
	 * @param args the command line arguments.
	 */
	public static void main(String[] args) {
		try {
			TCPClient client = new TCPClient();
			//noinspection InfiniteLoopStatement
			while (true)
				client.mainLoop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
