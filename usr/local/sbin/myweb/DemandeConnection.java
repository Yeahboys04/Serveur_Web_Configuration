import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
            boolean arret = false;
            int i= 0;
            while(!arret && i!=ipAccepter.size()){
                String adresseReseau = ipAccepter.get(i).split("/")[0];
                try{
                    if(ipAccepter.get(i).split("/")[1]!=null){
                        int masque = Integer.parseInt(ipAccepter.get(i).split("/")[1]);
                        if(VerificateurAdresseIP.estDansReseau(clientIP,adresseReseau,masque)){
                            arret = true;
                        }
                        i++;
                    }
                }catch (ArrayIndexOutOfBoundsException e){
                    i++;
                }
            }
            if (!arret) {
                // Si c'est le cas, on écrit dans le fichier de log des erreurs
                boolean stop = false;
                int j = 0 ;
                while(!stop && j!=ipRefuser.size()){
                    String adress = ipRefuser.get(j).split("/")[0];
                    try{
                        if(ipRefuser.get(j).split("/")[1]!=null) {
                            int masque = Integer.parseInt(ipRefuser.get(j).split("/")[1]);
                            if (VerificateurAdresseIP.estDansReseau(clientIP, adress, masque)) {
                                erreurs.logError("Connexion refusée de l'IP : " + clientIP);
                                stop = true;
                            }
                            j++;
                        }
                    }catch (ArrayIndexOutOfBoundsException e){
                        j++;
                    }
                }
                if(!stop){
                    erreurs.logError("Connexion non accepté : " + clientIP);
                }

                clientSocket.close();
                return;
            }
            // On teste si l'IP est dans la liste des acceptées

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
                        envoyerReponse(clientSocket, contentType, content, isBase64);
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

    private void envoyerReponse(Socket clientSocket, String contentType, byte[] content, boolean isBase64) throws IOException {
        if (isBase64) {
            String base64Content = Base64.getEncoder().encodeToString(content);
            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "Content-Encoding: base64\r\n" +
                    "Content-Length: " + base64Content.length() + "\r\n\r\n";
            envoyerBytes(clientSocket, responseHeaders.getBytes());
            envoyerBytes(clientSocket, base64Content.getBytes());
        } else {
            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "Content-Length: " + content.length + "\r\n\r\n";
            envoyerBytes(clientSocket, responseHeaders.getBytes());
            envoyerBytes(clientSocket, content);
        }
    }

    private void envoyerBytes(Socket clientSocket, byte[] bytes) throws IOException {
        OutputStream out = clientSocket.getOutputStream();
        out.write(bytes);
        out.flush();
    }
}
