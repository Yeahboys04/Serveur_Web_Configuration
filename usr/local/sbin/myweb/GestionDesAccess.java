import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class GestionDesAccess {
    private String accessLogPath;

    public GestionDesAccess(String accessLogPath) {
        this.accessLogPath = accessLogPath;
    }

    public void logAccess(String message) {
        try (FileWriter fw = new FileWriter(accessLogPath, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
