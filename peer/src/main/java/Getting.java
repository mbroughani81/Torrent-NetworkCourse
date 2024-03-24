import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;


public class Getting {
    static int startGetting (String filename, String trackerAddress, String listenAddress) throws GettingFailed {
        int clientPort = 19001;

        int result = -1;
        try {
            DatagramSocket datagramSocket = new DatagramSocket(new InetSocketAddress("127.0.0.1", new ServerSocket(0).getLocalPort()));
            datagramSocket.setSoTimeout(2000);

            String[] addr1 = trackerAddress.split(":");
            SocketAddress serverAddress = new InetSocketAddress(addr1[0], Integer.parseInt(addr1[1]));

            byte[] requestBytes = new byte[1024];

            String type = "get\n";
            String peerID = "\n";
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
                System.out.println("Sending get request to tracker. number of tries : " + (t + 1));
                DatagramPacket request = new DatagramPacket(requestBytes, requestBytes.length, serverAddress);
                datagramSocket.send(request);
                
                try {
                    DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
                    datagramSocket.receive(response);
                    Response r = Utility.parseReponse(response.getData());
                    System.out.println("Final response =>" + r.peerId + " " + Arrays.toString(r.peers));

                    File file = new File(r.peerId + "/" + filename); 
                    System.out.println(r.peers.length + " peers count");
                    
                    if (r.peers.length == 0) {
                        System.out.println("File is not shared by any peer!");
                        break;
                    } else {
                        getFile(file, r.peers);
                        result = r.peerId;
                        break;
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Cannot get any response from Tracker.");
                    System.out.println("Retrying...");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (result == -1) {
            throw new GettingFailed();
        }
        return result;
    }

    static void getFile(File resultFile, String[] peers) throws GettingFailed {
        try {
            int rand = Math.abs(new Random().nextInt()) % peers.length;
            System.out.println("seleceted peer => " + peers[rand]);
            System.out.println("all peers => " + Arrays.toString(peers));

            String[] addr = peers[rand].split(":");
            System.out.println("Address to start download from here => " + addr[0] + " " + addr[1]);
        
            Socket socket = new Socket("127.0.0.1", Integer.parseInt(addr[1]));    
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            byte[] b = new byte[1024];
            int ll = inputStream.read(b);
            byte[] bb = new byte[ll];
            for (int i = 0; i < ll; i++)
                bb[i] = b[i];
            resultFile.getParentFile().mkdirs();
            resultFile.createNewFile();
            Files.write(resultFile.toPath(), bb);
            System.out.println(resultFile.toPath() + " is downloaded!");
        } catch (IOException e) {
            e.printStackTrace();
            throw new GettingFailed();
        }
    }
}

class GettingFailed extends Exception {

    public GettingFailed() {
        super("failure in getting the file!");
    }
    
} 