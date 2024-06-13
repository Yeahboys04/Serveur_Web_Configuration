import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpServer {
    private ServerSocket serverSocket;
    private String rootDirectory;
    private AccessLogger accessLogger;
    private ErrorLogger errorLogger;
    private RequestHandler requestHandler;

    public HttpServer(int port, String rootDirectory, String accessLogPath, String errorLogPath, List<String> acceptedIPs, List<String> rejectedIPs) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.rootDirectory = rootDirectory;
        this.accessLogger = new AccessLogger(accessLogPath);
        this.errorLogger = new ErrorLogger(errorLogPath);
        this.requestHandler = new RequestHandler(rootDirectory, acceptedIPs, rejectedIPs);
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                requestHandler.handleRequest(clientSocket, accessLogger, errorLogger);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Load configuration from XML file
            File xmlFile = new File("config/myweb.conf");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Read configuration values
            int port = Integer.parseInt(doc.getElementsByTagName("port").item(0).getTextContent());
            String rootDir = doc.getElementsByTagName("root").item(0).getTextContent();
            String accessLogPath = doc.getElementsByTagName("accesslog").item(0).getTextContent();
            String errorLogPath = doc.getElementsByTagName("errorlog").item(0).getTextContent();
            List<String> acceptedIPs = Stream.of(doc.getElementsByTagName("accept").item(0).getTextContent().split(","))
                    .collect(Collectors.toList());
            List<String> rejectedIPs = Stream.of(doc.getElementsByTagName("reject").item(0).getTextContent().split(","))
                    .collect(Collectors.toList());

            // Initialize server
            HttpServer server = new HttpServer(port, rootDir, accessLogPath, errorLogPath, acceptedIPs, rejectedIPs);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
