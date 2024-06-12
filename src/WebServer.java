import java.net.*;
import java.io.*;

public class WebServer {
    private ConfigLoader config;

    public WebServer(ConfigLoader config) {
        this.config = config;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(config.getPort())) {
            System.out.println("Server started on port " + config.getPort());
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, config)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
