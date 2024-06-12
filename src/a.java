import java.nio.file.StandardOpenOption;
import java.nio.file.Paths;

private void logAccess(String logMessage) {
    try {
        if (config.getAccessLog() != null) {
            Files.write(Paths.get(config.getAccessLog()), (logMessage + "\n").getBytes(), StandardOpenOption.APPEND);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

private void logError(String logMessage) {
    try {
        if (config.getErrorLog() != null) {
            Files.write(Paths.get(config.getErrorLog()), (logMessage + "\n").getBytes(), StandardOpenOption.APPEND);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
