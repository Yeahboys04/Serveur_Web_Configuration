import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

public class DemandeConnection {
    private String cheminDaccess;
    private List<String> ipAccepter;
    private List<String> ipRefuser;

    public DemandeConnection(String chemin, List<String> acceptedIPs, List<String> rejectedIPs) {
        this.cheminDaccess = chemin;
        this.ipAccepter = acceptedIPs;
        this.ipRefuser = rejectedIPs;
    }

    public void handleRequest(Socket clientSocket, GestionDesAccess access, GestionDesErrors erreurs) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", "./var/www/status.sh");
        try {
            Process process = processBuilder.start();
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            // On récupère l'adresse IP du client
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            // On teste si l'IP est dans la liste des refusées
            if (ipRefuser.contains(clientIP)) {
                // Si c'est le cas, on écrit dans le fichier de log des erreurs
                erreurs.logError("Connexion refusée de l'IP : " + clientIP);
                clientSocket.close();
                return;
            }
            // On teste si l'IP est dans la liste des acceptées
            if (!ipAccepter.contains(clientIP)) {
                // Si ce n'est pas le cas
                erreurs.logError("Connexion non acceptée de : " + clientIP);
                clientSocket.close();
                return;
            }

            String requete = lireRequete(clientSocket);
            if (requete != null) {
                access.logAccess("Requête: " + requete);
                System.out.println(requete);
                String[] requestParts = separationRequete(requete);
                if ("GET".equals(requestParts[0])) {
                    String cheminFichier = cheminDaccess + requestParts[1];
                    if (requestParts[1].equals("/")) {
                        cheminFichier = cheminDaccess + "/index.html";
                    }
                    Path path = Paths.get(cheminFichier);
                    if (Files.exists(path)) {
                        byte[] content = Files.readAllBytes(path);
                        String contentType = Files.probeContentType(path);
                        if (contentType == null) {
                            contentType = "text/html";
                        }
                        boolean isBase64 = estFichierBinaire(contentType);
                        if (isBase64) {
                            envoyerReponseBase64(clientSocket, contentType, content);
                        } else {
                            envoyerReponseTexte(clientSocket, contentType, cheminFichier);
                        }
                    } else {
                        String messageErreur = "HTTP/1.1 404 Not Found\r\n\r\n";
                        envoyerBytes(clientSocket, messageErreur.getBytes());
                        erreurs.logError("Fichier non trouvé : " + cheminFichier);
                        System.out.println("Fichier non trouvé");
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

    private String lireRequete(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        return in.readLine();
    }

    private String[] separationRequete(String requete) {
        return requete.split(" ");
    }

    private boolean estFichierBinaire(String contentType) {
        return contentType.startsWith("images/") || contentType.startsWith("audio/") || contentType.startsWith("videos/");
    }

    private void envoyerReponseBase64(Socket clientSocket, String contentType, byte[] content) throws IOException {
        String base64Content = Base64.getEncoder().encodeToString(content);
        String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Encoding: base64\r\n" +
                "Content-Length: " + base64Content.length() + "\r\n\r\n";
        envoyerBytes(clientSocket, responseHeaders.getBytes());
        envoyerBytes(clientSocket, base64Content.getBytes());
    }

    private void envoyerReponseTexte(Socket clientSocket, String contentType, String cheminFichier) throws IOException {
        FileReader fr = new FileReader(cheminFichier);
        BufferedReader fichier = new BufferedReader(fr);
        String ligne = fichier.readLine();
        String codeHtml = "";
        while (ligne != null) {
            if (ligne.contains("<code")) {
                boolean debutInterpreteur = false;
                String interpreteur = "";
                for (int i = 0; i < ligne.length(); i++) {
                    if (ligne.charAt(i) == '\"') {
                        debutInterpreteur = !debutInterpreteur;
                    } else if (debutInterpreteur) {
                        interpreteur += ligne.charAt(i);
                    }
                }
                String code = "\"";
                ligne = fichier.readLine();
                while (!ligne.contains("</code>")) {
                    code += ligne;
                    ligne = fichier.readLine();
                }
                code += "\"";
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command(interpreteur, "-c", code);
                try {
                    Process process = processBuilder.start();
                    process.waitFor();
                    StringBuilder output = new StringBuilder();

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line + "\n");
                    }
                    reader.close();

                    int exitVal = process.waitFor();
                    if (exitVal == 0) {
                        codeHtml += output + " ";
                        System.out.println("output : " + output);
                    } else {
                        System.out.println("échec");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                codeHtml += ligne + " ";
            }
            ligne = fichier.readLine();
        }
        fichier.close();
        Path path = Paths.get(cheminFichier);
        byte[] content = Files.readAllBytes(path);
        String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + content.length + "\r\n\r\n";
        envoyerBytes(clientSocket, responseHeaders.getBytes());
        envoyerBytes(clientSocket, codeHtml.getBytes());
    }

    private void envoyerBytes(Socket clientSocket, byte[] bytes) throws IOException {
        OutputStream out = clientSocket.getOutputStream();
        out.write(bytes);
        out.flush();
    }
}
