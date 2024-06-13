import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ErrorLogger {
    private String errorLogPath;

    public ErrorLogger(String errorLogPath) {
        this.errorLogPath = errorLogPath;
    }

    public void logError(String message) {
        try (FileWriter fw = new FileWriter(errorLogPath, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

