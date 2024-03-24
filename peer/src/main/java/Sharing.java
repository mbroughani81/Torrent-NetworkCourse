import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.Arrays;

public class Sharing {
    static void startSharing(String peerId, String filepath, String trackerAddress, String listenAddress) {
        // infrom tracker about sharinga
        int clientPort = 19000;
        try {
            DatagramSocket datagramSocket = new DatagramSocket(
                    new InetSocketAddress("127.0.0.1", new ServerSocket(0).getLocalPort()));
            datagramSocket.setSoTimeout(2000);

            String[] addr1 = trackerAddress.split(":");
            SocketAddress serverAddress = new InetSocketAddress(addr1[0], Integer.parseInt(addr1[1]));

            byte[] requestBytes = new byte[1024];

            System.out.println("peerId is => " + peerId);
            File file = new File(filepath);
            String filename = file.getName();
            String type = "share\n";
            String peerID = peerId + "\n";
            String fname = filename + "\n";
            String lAddress = listenAddress + "\n";

            int i = 0;
            byte[] b = type.getBytes();
            for (int t = 0; t < b.length; t++, i++) {
                requestBytes[i] = b[t];
            }
            b = peerID.getBytes();
            for (int t = 0; t < b.length; t++, i++) {
                requestBytes[i] = b[t];
            }
            b = fname.getBytes();
            for (int t = 0; t < b.length; t++, i++) {
                requestBytes[i] = b[t];
            }
            b = lAddress.getBytes();
            for (int t = 0; t < b.length; t++, i++) {
                requestBytes[i] = b[t];
            }

            for (int t = 0; t < 5; t++) {
                // now listen for a while, check if tracker answers.
                try {
                    serveFile(file, listenAddress);

                    System.out.println("Sending share request to tracker. number of tries : " + t);
                    DatagramPacket request = new DatagramPacket(requestBytes, requestBytes.length, serverAddress);
                    datagramSocket.send(request);

                    DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
                    datagramSocket.receive(response);
                    Response r = Utility.parseReponse(response.getData());
                    System.out.println("Final response =>" + r.peerId + " " + Arrays.toString(r.peers));

                    Thread t1 = createHeartBeatThread(file, datagramSocket, serverAddress, String.valueOf(r.peerId));
                    t1.start();
                    break;
                } catch (java.net.BindException e) {
                    System.out.println("Cannot bind to the tcp port. Abort sharing process...");
                    e.printStackTrace();
                    break;
                } catch (SocketTimeoutException e) {
                    System.out.println("Cannot get any response from Tracker.");
                    System.out.println("Retrying...");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Thread createHeartBeatThread(final File file, 
            final DatagramSocket datagramSocket, 
            final SocketAddress socketAddress, 
            final String peerId) {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.interrupted()) {
                    // send a upd requeust to tracker, with peerId,
                    byte[] requestBytes = new byte[1024];
                    String type = "heartbeat\n";
                    String peerID = peerId + "\n";
                    String fname = file.getName() + "\n";
                    String lAddress = "\n";

                    int i = 0;
                    byte[] b = type.getBytes();
                    for (int t = 0; t < b.length; t++, i++) {
                        requestBytes[i] = b[t];
                    }
                    b = peerID.getBytes();
                    for (int t = 0; t < b.length; t++, i++) {
                        requestBytes[i] = b[t];
                    }
                    b = fname.getBytes();
                    for (int t = 0; t < b.length; t++, i++) {
                        requestBytes[i] = b[t];
                    }
                    b = lAddress.getBytes();
                    for (int t = 0; t < b.length; t++, i++) {
                        requestBytes[i] = b[t];
                    }

                    System.out.println("Sending Heartbeat(I am alive!)");

                    DatagramPacket packet = new DatagramPacket(requestBytes, requestBytes.length, socketAddress);
                    try {
                        datagramSocket.send(packet);
                    } catch (IOException e) {}
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        return thread;
    }

    static void serveFile(final File file, final String listenAddress) throws IOException {
        String[] addr = listenAddress.split(":");
        System.out.println("ADDR => " + Arrays.toString(addr));
        final ServerSocket serverSocket = new ServerSocket(Integer.parseInt(addr[1]));
        // listen on listenAddress, and serve the file!
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        Socket socket = serverSocket.accept();

                        System.out.println("FIRST => ");
                        System.out.println(socket.getRemoteSocketAddress());
                        System.out.println("SECOND => ");
                        System.out.println(socket.getLocalSocketAddress());

                        FileServer fileServer = new FileServer(file, socket);
                        Thread thread = new Thread(fileServer);
                        thread.start();
                    }
                } catch (IOException e) {}
            }
        });
        t1.start();
    }
}

class FileServer implements Runnable {

    File file;
    Socket socket;

    public FileServer(File file, Socket socket) {
        this.file = file;
        this.socket = socket;
    }

    public void run() {
        // Now, we should send to file, using the socket.

        try {
            byte[] b = Files.readAllBytes(file.toPath());

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.write(b);
            outputStream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}