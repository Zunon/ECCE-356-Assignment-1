import java.net.*;
import java.io.*;
import java.util.Scanner;

public class TCPClient {
	public static final int PORT = 10001;
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
		return (stdIn.nextLine());
	}

	public void close() {
		try {
			socket.close();
			stdIn.close();
			netIn.close();
			netOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		try {
			TCPClient client = new TCPClient();
			String response = client.sendAndReceive( "Write any message: ");
			System.out.println("Message received successfully at " + response);
			response = client.sendAndReceive("Enter the file name with extension to get the size: ");
			System.out.println("The File size is: " + response + " Bytes.");
			client.close();
		} catch (UnknownHostException e) {
			System.out.println("Unknown host: " + e.getMessage());
		} catch (ConnectException e){
			System.out.println("Connection refused by host!");
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
}
