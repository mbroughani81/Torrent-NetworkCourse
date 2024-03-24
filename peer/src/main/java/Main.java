import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws SecurityException, FileNotFoundException, IOException {
        System.out.println(Arrays.toString(args));
        
        if (args.length == 0) {
            System.out.println("!!Starting Cli");
            
            Cli cli = new Cli();
            cli.start();
            return;
        }

        if (args[0].equals("share")) {
            System.out.println("!!Starting to share file");

            String filename = args[1];
            String trackerAddress = args[2];
            String listenAddress = args[3];
            
            Sharing.startSharing("",filename, trackerAddress, listenAddress);
        }
        if (args[0].equals("get")) {
            System.out.println("!!Starting to get file");
            
            String filename = args[1];
            String trackerAddress = args[2];
            String listenAddress = args[3];
            
            try {
                int peerId = Getting.startGetting(filename, trackerAddress, listenAddress);
                Sharing.startSharing(String.valueOf(peerId), peerId + "/" + filename, trackerAddress, listenAddress);
            } catch (GettingFailed e) {
                System.out.println("Cannot download the file");
                e.printStackTrace();
            }
        }
    }
}
