import java.util.ArrayList;

public class Utility {
    static Response parseReponse(byte[] responseBytes) {
        String responseStr = new String(responseBytes);
        String[] response = responseStr.split("\n");

        ArrayList<String> peers = new ArrayList<String>();
        for (String s : response[1].split(",")) {
            if (s.length() > 6) {
                peers.add(s);
            }
        }
        String[] p = new String[peers.size()];
        for (int i = 0; i < peers.size(); i++) {
            p[i] = peers.get(i);
        }

        return new Response(Integer.valueOf(response[0]), p);
    }
}

class Response {
    int peerId;
    String[] peers;
    
    public Response(int peerId, String[] peers) {
        this.peerId = peerId;
        this.peers = peers;
    }
}
