import java.net.*;
import java.io.*;
import java.util.Scanner;

public class TCPClient {
	public static final int PORT = 45632;
	Scanner stdIn = new Scanner(System.in);
	Scanner netIn = null;
	PrintWriter netOut = null;
	Socket socket = null;
	String serverName = null;
	String myName = InetAddress.getLocalHost().toString();
	TCPClient() throws IOException {
		System.out.println("TCP Client starting on host: " + myName);
		String serverName = prompt("Type name of TCP Server: ");
		socket = new Socket(serverName, PORT);
		DataInputStream inStream = new DataInputStream(socket.getInputStream());
		DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
		netIn = new Scanner(new InputStreamReader(inStream));
		netOut = new PrintWriter(outStream, true);
	}
	public String sendAndReceive(String prompt) throws IOException {
		String message = prompt(prompt);
		netOut.println(message);
		return (netIn.nextLine());
	}
	public String prompt(String message) {
		System.out.print(message);
		String str = stdIn.nextLine();
		if (str.equals("quit"))
			close();
		return (str);
	}

	public void mainLoop() throws IOException {
		String response = sendAndReceive( "Write any message: ");
		System.out.println("Message received successfully at " + response);
		response = sendAndReceive("Enter the file name with extension to get the size: ");
		if (response.equals("File not found!"))
			System.out.println("File not found!");
		else
			System.out.println("The File size is: " + response + " Bytes.");
		mainLoop();
	}

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
	public static void main(String[] args) {
		try {
			TCPClient client = new TCPClient();
			client.mainLoop();
		} catch (UnknownHostException e) {
			System.out.println("Unknown host: " + e.getMessage());
		} catch (ConnectException e){
			System.out.println("Connection refused by host!");
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
}
