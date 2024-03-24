import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Main {

    public static void main(String[] args) throws SecurityException, FileNotFoundException, IOException {
        System.out.println(Arrays.toString(args));

        if (args.length == 0) {
            System.out.println("!!Starting Cli");

            Cli cli = new Cli();
            cli.start();
            return;
        }

        System.out.println("!!Starting tracker");

        String[] address = args[0].split(":");
        int port = Integer.parseInt(address[1]);
        String host = address[0];

        try {
            BlockingQueue<Command> queue = new ArrayBlockingQueue<Command>(10);
            DatagramSocket socket = new DatagramSocket(new InetSocketAddress(host, port));
            
            Tracker tracker = new Tracker(queue, socket);
            Endpoint packetReceiver = new Endpoint(queue, socket);

            Thread t1 = new Thread(tracker);
            t1.start();
            packetReceiver.startReceiving();        

        } catch (SocketException e) {
            System.out.println("Error setting up tracker");
            e.printStackTrace();
        }
    }
}
