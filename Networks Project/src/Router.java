import java.io.IOException;
import java.net.*;

public class Router {

    private DatagramSocket receiveSocket;
    private DatagramSocket sendSocket;
    private int[] ports = {20234, 20235, 45632, 23654};

    public Router() throws SocketException {
        receiveSocket = new DatagramSocket(ports[0]);
        sendSocket = new DatagramSocket();
    }

    public void forwardPacket() throws IOException {
        byte[] buffer = new byte[4096];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        receiveSocket.receive(packet);

        // simulate 10% packet loss
        if (Math.random() < 0.1) {
            System.out.println("Packet dropped: " + new String(packet.getData(), 0, packet.getLength()));
            return;
        }

        InetAddress destAddress = packet.getAddress();
        DatagramPacket sendPacket = new DatagramPacket(packet.getData(), packet.getLength(), destAddress, ports[2]);
        sendSocket.send(sendPacket);
        System.out.println("Packet forwarded: " + new String(packet.getData(), 0, packet.getLength()));
    }

    public void close() {
        receiveSocket.close();
        sendSocket.close();
    }

    public static void main(String[] args) throws IOException {
        // create a router instance
        Router router = new Router();

        // forward packets until user interrupts the program
        while (true) {
            try {
                router.forwardPacket();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        // close the sockets
        router.close();
    }
}