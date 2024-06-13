import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RequestHandler {
    private String rootDirectory;
    private List<String> acceptedIPs;
    private List<String> rejectedIPs;

    public RequestHandler(String rootDirectory, List<String> acceptedIPs, List<String> rejectedIPs) {
        this.rootDirectory = rootDirectory;
        this.acceptedIPs = acceptedIPs;
        this.rejectedIPs = rejectedIPs;
    }

    public void handleRequest(Socket clientSocket, AccessLogger accessLogger, ErrorLogger errorLogger) {
        try {
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            if (rejectedIPs.contains(clientIP)) {
                errorLogger.logError("Rejected connection from: " + clientIP);
                clientSocket.close();
                return;
            }

            if (!acceptedIPs.contains(clientIP)) {
                errorLogger.logError("Connection from non-accepted IP: " + clientIP);
                clientSocket.close();
                return;
            }

            String requestLine = readRequest(clientSocket);
            if (requestLine != null) {
                accessLogger.logAccess("Request: " + requestLine);
                System.out.println(requestLine);
                String[] requestParts = splitRequest(requestLine);
                if ("GET".equals(requestParts[0])) {
                    String filePath = rootDirectory + requestParts[1];
                    Path path = Paths.get(filePath);
                    if (Files.exists(path)) {
                        byte[] content = Files.readAllBytes(path);
                        String contentType = Files.probeContentType(path);
                        if (contentType == null) {
                            contentType = "text/html";
                        }
                        sendResponse(clientSocket, contentType, content);
                    } else {
                        String errorMessage = "HTTP/1.1 404 Not Found\r\n\r\n";
                        sendBytes(clientSocket, errorMessage.getBytes());
                        errorLogger.logError("File not found: " + filePath);
                        System.out.println("Fichier pas trouvé");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String readRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        return in.readLine();
    }

    private String[] splitRequest(String requestLine) {
        return requestLine.split(" ");
    }

    private void sendResponse(Socket clientSocket, String contentType, byte[] content) throws IOException {
        String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + content.length + "\r\n\r\n";
        sendBytes(clientSocket, responseHeaders.getBytes());
        sendBytes(clientSocket, content);
    }

    private void sendBytes(Socket clientSocket, byte[] bytes) throws IOException {
        OutputStream out = clientSocket.getOutputStream();
        out.write(bytes);
        out.flush();
    }
}

