import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ConfigLoader config;

    public ClientHandler(Socket clientSocket, ConfigLoader config) {
        this.clientSocket = clientSocket;
        this.config = config;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            String requestLine = in.readLine();
            if (requestLine == null || !requestLine.startsWith("GET")) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length != 3) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            String filePath = config.getRoot() + requestParts[1];
            File file = new File(filePath);
            if (!file.exists()) {
                sendErrorResponse(out, 404, "Not Found");
                return;
            }

            sendFileResponse(out, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendErrorResponse(BufferedWriter out, int statusCode, String message) throws IOException {
        out.write("HTTP/1.1 " + statusCode + " " + message + "\r\n");
        out.write("Content-Type: text/html\r\n");
        out.write("\r\n");
        out.write("<html><body><h1>" + message + "</h1></body></html>\r\n");
        out.flush();
    }

    private void sendFileResponse(BufferedWriter out, File file) throws IOException {
        out.write("HTTP/1.1 200 OK\r\n");
        out.write("Content-Type: " + getContentType(file) + "\r\n");
        out.write("\r\n");
        out.flush();

        try (BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(new String(buffer, 0, bytesRead));
            }
        }
        out.flush();
    }

    private String getContentType(File file) {
        String fileName = file.getName();
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".mp3")) return "audio/mpeg";
        // Add more types as needed
        return "application/octet-stream";
    }

// Encodage des fichiers multimédia en base64
//    private void sendFileResponse(BufferedWriter out, File file) throws IOException {
//        String contentType = getContentType(file);
//        if (file.getName().endsWith(".html")) {
//            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
//            content = processDynamicCode(content);
//            out.write("HTTP/1.1 200 OK\r\n");
//            out.write("Content-Type: text/html\r\n");
//            out.write("\r\n");
//            out.write(content);
//        } else {
//            // Existing code for base64 encoding
//        }
//        out.flush();
//    }
//
//    private String processDynamicCode(String content) {
//        Pattern pattern = Pattern.compile("<code interpreteur=\"(.*?)\">(.*?)</code>", Pattern.DOTALL);
//        Matcher matcher = pattern.matcher(content);
//        StringBuffer sb = new StringBuffer();
//
//        while (matcher.find()) {
//            String interpreter = matcher.group(1);
//            String code = matcher.group(2).trim();
//            String result = executeCode(interpreter, code);
//            matcher.appendReplacement(sb, result);
//        }
//        matcher.appendTail(sb);
//        return sb.toString();
//    }
//
//    private String executeCode(String interpreter, String code) {
//        try {
//            ProcessBuilder pb = new ProcessBuilder(interpreter);
//            Process process = pb.start();
//            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
//            writer.write(code);
//            writer.close();
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            StringBuilder output = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                output.append(line).append("\n");
//            }
//            return output.toString();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return "Error executing code";
//        }
//    }


// Exécution de code dynamique dans les pages HTML

//    private void sendFileResponse(BufferedWriter out, File file) throws IOException {
//        String contentType = getContentType(file);
//        if (file.getName().endsWith(".html")) {
//            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
//            content = processDynamicCode(content);
//            out.write("HTTP/1.1 200 OK\r\n");
//            out.write("Content-Type: text/html\r\n");
//            out.write("\r\n");
//            out.write(content);
//        } else {
//            // Existing code for base64 encoding
//        }
//        out.flush();
//    }
//
//    private String processDynamicCode(String content) {
//        Pattern pattern = Pattern.compile("<code interpreteur=\"(.*?)\">(.*?)</code>", Pattern.DOTALL);
//        Matcher matcher = pattern.matcher(content);
//        StringBuffer sb = new StringBuffer();
//
//        while (matcher.find()) {
//            String interpreter = matcher.group(1);
//            String code = matcher.group(2).trim();
//            String result = executeCode(interpreter, code);
//            matcher.appendReplacement(sb, result);
//        }
//        matcher.appendTail(sb);
//        return sb.toString();
//    }
//
//    private String executeCode(String interpreter, String code) {
//        try {
//            ProcessBuilder pb = new ProcessBuilder(interpreter);
//            Process process = pb.start();
//            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
//            writer.write(code);
//            writer.close();
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            StringBuilder output = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                output.append(line).append("\n");
//            }
//            return output.toString();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return "Error executing code";
//        }
//    }


// Gestion des journaux d'accès et d'erreurs
//    import java.nio.file.StandardOpenOption;
//import java.nio.file.Paths;
//
//    private void logAccess(String logMessage) {
//        try {
//            if (config.getAccessLog() != null) {
//                Files.write(Paths.get(config.getAccessLog()), (logMessage + "\n").getBytes(), StandardOpenOption.APPEND);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void logError(String logMessage) {
//        try {
//            if (config.getErrorLog() != null) {
//                Files.write(Paths.get(config.getErrorLog()), (logMessage + "\n").getBytes(), StandardOpenOption.APPEND);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
