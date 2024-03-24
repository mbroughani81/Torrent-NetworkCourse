import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Logging {
    static Map<String, FileOutputStream> mp = new HashMap<String,FileOutputStream>();

    static void log(String filename, String s) {
        if (mp.get(filename) == null) {
            try {
                FileOutputStream outputStream = new FileOutputStream(filename + ".log");
                mp.put(filename, outputStream);
            } catch (FileNotFoundException e) {}
        }
        String ss = s + "\n";
        try {
            mp.get(filename).write(ss.getBytes());
        } catch (IOException e) {};
    }
}
