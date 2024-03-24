import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class Cli {
    void start() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(">> ");
            String line = scanner.nextLine();
            String[] ll = line.split(" ");
            if (line.equals("request logs")) {
                List<String> log;
                try {
                    log = Files.readAllLines(Paths.get("all.log"));
                    for (String s : log) {
                        System.out.println(s);
                    }
                } catch (IOException e) {}
            }
            if (ll.length >= 2 && ll[0].equals("file_logs") && ll[1].equals("all")) {

            }
        }
    }
}
