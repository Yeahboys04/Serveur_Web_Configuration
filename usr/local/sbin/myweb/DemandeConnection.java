import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
                    if (cheminFichier.equals("var/www/status.html")) {
                        ProcessBuilder processBuilder = new ProcessBuilder();
                        processBuilder.command("bash", "-c", "./var/www/status.sh");
                        try {
                            Process process = processBuilder.start();
                            process.waitFor();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (requestParts[1].equals("/")) {
                        cheminFichier = cheminDaccess + "/index.html";
                    }
                    Path path = Paths.get(cheminFichier);
                    System.out.println(cheminFichier);
                    if (Files.exists(path)) {
                        String contentType = Files.probeContentType(path);
                        if (contentType == null) {
                            contentType = "text/html";
                        }
                        boolean isBase64 = estFichierBinaire(contentType);
                        if (!isBase64) {
                            envoyerReponseTexte(clientSocket, contentType, cheminFichier);
                        }
                  //      envoyerReponse(clientSocket, contentType, content, isBase64);
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
        System.out.println(contentType);
        return contentType.endsWith(".png") || contentType.endsWith("jpeg") || contentType.endsWith(".gif");
    }

    private String envoyerReponseBase64(Socket clientSocket, String contentType, byte[] content) throws IOException {
        String base64Content = Base64.getEncoder().encodeToString(content);
        base64Content = "data:" + contentType + ";base64," + base64Content;
        return base64Content;
    }

    private void envoyerReponseTexte(Socket clientSocket, String contentType, String cheminFichier) throws IOException {
        FileReader fr = new FileReader(cheminFichier);
        BufferedReader fichier = new BufferedReader(fr);
        String ligne = fichier.readLine();
        String codeHtml = "";
        while (ligne != null) {
            if (ligne.contains("<img")) {
                boolean debutAttribut = false;
                String image = "";
                for (int i = 0; i < ligne.length(); i++) {
                    if (ligne.charAt(i) == '\"') {
                        debutAttribut = !debutAttribut;
                    } else if (debutAttribut) {
                        image += ligne.charAt(i);
                    }
                }
                Path path = Paths.get("var/www/" + image);
                String nouvelleImage = "";
                if (Files.exists(path)) {
                    byte[] content = Files.readAllBytes(path);
                    nouvelleImage = envoyerReponseBase64(clientSocket, contentType, content);
                }
                ligne = ligne.replace(image, nouvelleImage);
                codeHtml += ligne;
                System.out.println(codeHtml);
            } else if (ligne.contains("<code")) {
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
        System.out.println(codeHtml);
        fichier.close();
        byte[] content = codeHtml.getBytes();
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