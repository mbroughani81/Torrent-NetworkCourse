import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Tracker implements Runnable {
    BlockingQueue<Command> queue;
    Map<String, List<Peer>> activeFilesToPeer;
    Map<Integer, LocalDateTime> lastHeartbeat;
    DatagramSocket socket;

    public Tracker(BlockingQueue<Command> queue, DatagramSocket socket) {
        this.queue = queue;
        this.activeFilesToPeer = new HashMap<String,List<Peer>>();
        this.lastHeartbeat = new HashMap<Integer,LocalDateTime>();
        this.socket = socket;
    }

    public void run() {
        // start a thread, which check that updates all of activeFilesToPeer in 
        // time intervals with new hashmap that dosen't have the discarded record.
        Lock lock = new ReentrantLock();
        ActiveFilesToPeerRefresher refresher = new ActiveFilesToPeerRefresher(activeFilesToPeer, lastHeartbeat, lock);
        Thread t1 = new Thread(refresher);
        t1.start();

        while (true) {
            if (!queue.isEmpty()) {
                Command c = queue.poll();
                if (c instanceof Command.StartShare) {
                    Command.StartShare newC = (Command.StartShare)c;
                    lock.lock();
                    lastHeartbeat.put(newC.peerId, LocalDateTime.now());

                    if (activeFilesToPeer.get(newC.filename) == null) {
                        activeFilesToPeer.put(newC.filename, new ArrayList<Peer>());
                    }
                    List<Peer> peers = activeFilesToPeer.get(newC.filename);
                    peers.add(new Peer(newC.peerId, newC.peerTCPSocketAddress));
                    
                    Logging.log("all", "peerId : " + newC.peerId + " " + newC.peerTCPSocketAddress);
                    Logging.log("all", "Share " + newC.filename);
                    Logging.log("all", peers.toString());
                    Logging.log("all", "\n");

                    Logging.log("file_" + newC.filename, "peer " + newC.peerId + " started sharing!");
                    Logging.log("file_" + newC.filename, peers.toString());
                    Logging.log("file_" + newC.filename, "\n");

                    lock.unlock();
                

                    sendReponse(newC.peerId, new ArrayList<Peer>(), newC.peerUDPSocketAddress, socket);
                }
                if (c instanceof Command.StartGet) {
                    Command.StartGet newC = (Command.StartGet)c;

                    lock.lock();
                    lastHeartbeat.put(newC.peerId, LocalDateTime.now());

                    List<Peer> peers = activeFilesToPeer.get(newC.filename);
                    sendReponse(newC.peerId, peers, newC.peerUDPSocketAddress, socket);

                    Logging.log("all", "peerId : " + newC.peerId + " " + newC.peerTCPSocketAddress);
                    Logging.log("all", "Get " + newC.filename);
                    Logging.log("all", peers.toString());
                    Logging.log("all", "\n");

                    Logging.log("file_" + newC.filename, "peer " + newC.peerId + " requested file!");
                    Logging.log("file_" + newC.filename, peers.toString());
                    Logging.log("file_" + newC.filename, "\n");

                    lock.unlock();
                }
                if (c instanceof Command.HeartbeatSignal) {
                    Command.HeartbeatSignal newC = (Command.HeartbeatSignal)c;

                    lock.lock();
                    lastHeartbeat.put(newC.peerId, LocalDateTime.now());
                    lock.unlock();

                    sendReponse(newC.peerId, new ArrayList<Peer>(), newC.peerUDPSocketAddress, socket);
                }

            }
        }
    }

    static void sendReponse(int peerId, List<Peer> peers, InetSocketAddress peerUDPSocketAddress, DatagramSocket socket) {
        byte[] responseBytes = new byte[1024];
        
        int i = 0;
        String peerIdStr = String.valueOf(peerId) + "\n";
        byte[] b = peerIdStr.getBytes();
        for (int t = 0; t < b.length; t++, i++) {
            responseBytes[i] = b[t];
        }


        String peersListStr = "";
        if (peers != null) {
            for (Peer peer : peers) {
                if (peersListStr.length() > 0) {
                    peersListStr = peersListStr + ",";
                }
                peersListStr = peersListStr + peer.socketAddress.getAddress() + ":" + peer.socketAddress.getPort();
            }
        }
        peersListStr = peersListStr + "\n";
        b = peersListStr.getBytes();
        for (int t = 0; t < b.length; t++, i++) {
            responseBytes[i] = b[t];
        }
        
        DatagramPacket response = new DatagramPacket(responseBytes, responseBytes.length, peerUDPSocketAddress);
        try {
            socket.send(response);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

class Peer {
    int id;
    InetSocketAddress socketAddress;

    public Peer(int id, InetSocketAddress socketAddress) {
        this.id = id;
        this.socketAddress = socketAddress;
    }

    @Override
    public String toString() {
        return "Peer [id=" + id + "]";
    }
}

class ActiveFilesToPeerRefresher implements Runnable {

    Map<String, List<Peer>> activeFilesToPeer;
    Map<Integer, LocalDateTime> lastHeartbeat;
    Lock lock;

    public ActiveFilesToPeerRefresher(Map<String, List<Peer>> activeFilesToPeer,
            Map<Integer, LocalDateTime> lastHeartbeat,
            Lock lock) {
        this.activeFilesToPeer = activeFilesToPeer;
        this.lastHeartbeat = lastHeartbeat;
        this.lock = lock;
    }

    public void run() {
        while (true) {
            // if more than 5 seconds passed, than discard the peer.
            lock.lock();
            for (String filename : activeFilesToPeer.keySet()) {
                List<Peer> peers = activeFilesToPeer.get(filename);
                List<Peer> newPeers = new ArrayList<Peer>();
                for (Peer p : peers) {
                    if (ChronoUnit.SECONDS.between(lastHeartbeat.get(p.id), LocalDateTime.now()) < 10) {
                        newPeers.add(p);
                    } else {
                        System.out.println("Heartbeat for peerId" + p.id + " stopped!");

                        Logging.log("file_" + filename, "peer " + p.id + " stopped sharing!");
                    }
                }
                if (peers.size() != newPeers.size()) {
                    Logging.log("file_" + filename, peers.toString());
                    Logging.log("file_" + filename, "\n");
                }
                activeFilesToPeer.put(filename, newPeers);
            }
            System.out.println("activeFilesToPeer => " + activeFilesToPeer);

            lock.unlock();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {}
        }
    }
}