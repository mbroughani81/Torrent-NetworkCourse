import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import java.util.logging.*;


public class Endpoint {
    BlockingQueue<Command> queue;
    DatagramSocket socket;
    static int PEER_ID = 1;

    public Endpoint(BlockingQueue<Command> queue, DatagramSocket socket) {
        this.queue = queue;
        this.socket = socket;
    }

    void startReceiving() {
        while (true) {
            try {
                // Protocol:
                // # request
                // Type -> {Share,} 
                // Peer ID (empty line if not assigned)
                // Filename (empty line if not assigned)
                // Peer address (ip:port) to get file
                // EmptyLine (this is important! otherwize, peeraddress will be messed up!)
                //
                // # response
                // peer ID
                // list of requested peers addresses    

                // request parsing
                DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
                socket.receive(request);

                byte[] requestBytes = request.getData();
                String requestStr = new String(requestBytes);
                String[] tokens = requestStr.split("\n");
                System.out.println("Received from client => " + Arrays.toString(tokens));

                Command c = parseTokens(tokens, new InetSocketAddress(request.getAddress(), request.getPort()));
                System.out.println("parsed token => " + c);
                if (c != null) {
                    queue.add(c);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static Command parseTokens(String[] tokens, InetSocketAddress peerUDPSocketAddress) {
        String type = tokens[0];
        String peerId = tokens[1];
        String filename = tokens[2];
        String peerAddress = tokens[3];
        
        if (type.equals("share")) {
            String[] addr = peerAddress.split(":");
            System.out.println(Arrays.toString(addr));
            System.out.println(addr[1].length()); // bullshit!!!

            InetSocketAddress peerTCPSocketAddress = new InetSocketAddress(addr[0], Integer.parseInt(addr[1]));
            return new Command.StartShare(peerId, filename, peerTCPSocketAddress, peerUDPSocketAddress);
        }

        if (type.equals("get")) {
            String[] addr = peerAddress.split(":");

            InetSocketAddress peerTCPSocketAddress = new InetSocketAddress(addr[0], Integer.parseInt(addr[1]));
            return new Command.StartGet(peerId, filename, peerTCPSocketAddress, peerUDPSocketAddress);
        }
        
        if (tokens[0].equals("heartbeat")) {
            return new Command.HeartbeatSignal(peerId, filename, peerUDPSocketAddress);
        }

        return null;
    }
}

class Command {

    int peerId;

    static class StartShare extends Command {
        String filename;
        InetSocketAddress peerTCPSocketAddress;
        InetSocketAddress peerUDPSocketAddress;

        public StartShare(String peerId, String filename, InetSocketAddress peerTCPSocketAddress, InetSocketAddress peerUDPSocketAddress) {
            if (peerId.length() > 0) {
                this.peerId = Integer.parseInt(peerId);
            } else {
                this.peerId = Endpoint.PEER_ID++;
            }
            this.filename = filename;
            this.peerTCPSocketAddress = peerTCPSocketAddress;
            this.peerUDPSocketAddress = peerUDPSocketAddress;
        }
    }

    static class StartGet extends Command {
        String filename;
        InetSocketAddress peerTCPSocketAddress;
        InetSocketAddress peerUDPSocketAddress;

        public StartGet(String peerId, String filename, InetSocketAddress peerTCPSocketAddress, InetSocketAddress peerUDPSocketAddress) {
            if (peerId.length() > 0) {
                this.peerId = Integer.parseInt(peerId);
            } else {
                this.peerId = Endpoint.PEER_ID++;
            }
            this.filename = filename;
            this.peerTCPSocketAddress = peerTCPSocketAddress;
            this.peerUDPSocketAddress = peerUDPSocketAddress;
        }
    }

    static class HeartbeatSignal extends Command {
        String filename;
        InetSocketAddress peerUDPSocketAddress;

        public HeartbeatSignal(String peerId, String filename, InetSocketAddress peerUDPSocketAddress) {
            if (peerId.length() > 0) {
                this.peerId = Integer.parseInt(peerId);
                this.peerUDPSocketAddress = peerUDPSocketAddress;
            } else {
                throw new RuntimeException("peerId must be valid!");
            }
            this.filename = filename;
        }
    }
}