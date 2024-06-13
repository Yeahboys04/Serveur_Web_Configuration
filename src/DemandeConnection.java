import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DemandeConnection {
    private String CheminDaccess;
    private List<String> IpAccepter;
    private List<String> IpRefuser;

    public DemandeConnection(String chemin, List<String> acceptedIPs, List<String> rejectedIPs) {
        this.CheminDaccess = chemin;
        this.IpAccepter = acceptedIPs;
        this.IpRefuser = rejectedIPs;
    }

    public void handleRequest(Socket clientSocket, GestionDesAccess access, GestionDesErrors erreurs) {
        try {
            //On recupère l'adresseIp du client
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            //On test si l'ip est dans la list des refuser
            if (IpRefuser.contains(clientIP)) {
                //Si c'est le cas on écrit dans le fichier error log
                erreurs.logError("Connection rejeter de l'Ip : " + clientIP);
                clientSocket.close();
                return;
            }
            //On test si l'ip est dans la liste des accepter
            if (!IpAccepter.contains(clientIP)) {
                //Si ce n'est pas le cas
                erreurs.logError("Connection non accepter de : " + clientIP);
                clientSocket.close();
                return;
            }

            String requette = lireRequette(clientSocket);
            if (requette != null) {
                access.logAccess("Requette: " + requette);
                System.out.println(requette);
                String[] requestParts = separationRequette(requette);
                if ("GET".equals(requestParts[0])) {
                    String cheminFichier = CheminDaccess + requestParts[1];
                    Path path = Paths.get(cheminFichier);
                    if (Files.exists(path)) {
                        byte[] content = Files.readAllBytes(path);
                        String contentType = Files.probeContentType(path);
                        if (contentType == null) {
                            contentType = "text/html";
                        }
                        envoyerReponse(clientSocket, contentType, content);
                    } else {
                        String MessageDerreur = "HTTP/1.1 404 Pas Trouve\r\n\r\n";
                        envoyerBytes(clientSocket, MessageDerreur.getBytes());
                        erreurs.logError("Fichier pas trouvé : " + cheminFichier);
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

    private String lireRequette(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        return in.readLine();
    }

    private String[] separationRequette(String requette) {
        return requette.split(" ");
    }

    private void envoyerReponse(Socket clientSocket, String contentType, byte[] content) throws IOException {
        String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + content.length + "\r\n\r\n";
        envoyerBytes(clientSocket, responseHeaders.getBytes());
        envoyerBytes(clientSocket, content);
    }

    private void envoyerBytes(Socket clientSocket, byte[] bytes) throws IOException {
        OutputStream out = clientSocket.getOutputStream();
        out.write(bytes);
        out.flush();
    }
}

