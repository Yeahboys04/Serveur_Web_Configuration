import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Classe DemandeConnection qui gère les connexions des clients, les autorise ou les refuse
 * selon les adresses IP spécifiées, et traite les requêtes HTTP.
 */
public class DemandeConnection {
    private String cheminDaccess;
    private List<String> ipAccepter;
    private List<String> ipRefuser;

    /**
     * Constructeur pour initialiser les chemins d'accès et les listes d'IP acceptées et refusées.
     *
     * @param chemin Chemin d'accès aux fichiers.
     * @param acceptedIPs Liste des adresses IP acceptées.
     * @param rejectedIPs Liste des adresses IP refusées.
     */
    public DemandeConnection(String chemin, List<String> acceptedIPs, List<String> rejectedIPs) {
        this.cheminDaccess = chemin;
        this.ipAccepter = acceptedIPs;
        this.ipRefuser = rejectedIPs;
    }

    /**
     * Méthode pour gérer les requêtes entrantes des clients.
     *
     * @param clientSocket Le socket client.
     * @param access Gestionnaire des accès.
     * @param erreurs Gestionnaire des erreurs.
     * @throws IOException En cas d'erreur d'entrée/sortie.
     */
    public void handleRequest(Socket clientSocket, GestionDesAccess access, GestionDesErrors erreurs) throws IOException {
        try {
            // On récupère l'adresse IP du client
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            // On teste si l'IP est dans la liste des refusées
            boolean arret = false;
            int i = 0;
            while (!arret && i != ipAccepter.size()) {
                String adresseReseau = ipAccepter.get(i).split("/")[0];
                try {
                    if (ipAccepter.get(i).split("/")[1] != null) {
                        int masque = Integer.parseInt(ipAccepter.get(i).split("/")[1]);
                        if (VerificateurAdresseIP.estDansReseau(clientIP, adresseReseau, masque)) {
                            arret = true;
                        }
                        i++;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    i++;
                }
            }
            if (!arret) {
                // Si c'est le cas, on écrit dans le fichier de log des erreurs
                boolean stop = false;
                int j = 0;
                while (!stop && j != ipRefuser.size()) {
                    String adress = ipRefuser.get(j).split("/")[0];
                    try {
                        if (ipRefuser.get(j).split("/")[1] != null) {
                            int masque = Integer.parseInt(ipRefuser.get(j).split("/")[1]);
                            if (VerificateurAdresseIP.estDansReseau(clientIP, adress, masque)) {
                                erreurs.logError("Connexion refusée de l'IP : " + clientIP);
                                stop = true;
                            }
                            j++;
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        j++;
                    }
                }
                if (!stop) {
                    erreurs.logError("Connexion non acceptée : " + clientIP);
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
                    if (cheminFichier.equals("/var/www/status.html")) {
                        ProcessBuilder processBuilder = new ProcessBuilder();
                        processBuilder.command("bash", "-c", "/var/www/status.sh");
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

    /**
     * Lit la requête du client.
     *
     * @param clientSocket Le socket client.
     * @return La requête sous forme de chaîne de caractères.
     * @throws IOException En cas d'erreur de lecture.
     */
    private String lireRequete(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        return in.readLine();
    }

    /**
     * Sépare la requête HTTP en différentes parties.
     *
     * @param requete La requête HTTP.
     * @return Un tableau de chaînes de caractères contenant les parties de la requête.
     */
    private String[] separationRequete(String requete) {
        return requete.split(" ");
    }

    /**
     * Vérifie si le fichier est binaire.
     *
     * @param contentType Le type de contenu du fichier.
     * @return Vrai si le fichier est binaire, faux sinon.
     */
    private boolean estFichierBinaire(String contentType) {
        System.out.println(contentType);
        return contentType.endsWith(".png") || contentType.endsWith("jpeg") || contentType.endsWith(".gif");
    }

    /**
     * Envoie une réponse en base64 pour les fichiers binaires.
     *
     * @param clientSocket Le socket client.
     * @param contentType Le type de contenu.
     * @param content Le contenu du fichier en bytes.
     * @return Le contenu encodé en base64 sous forme de chaîne de caractères.
     * @throws IOException En cas d'erreur d'entrée/sortie.
     */
    private String envoyerReponseBase64(Socket clientSocket, String contentType, byte[] content) throws IOException {
        String base64Content = Base64.getEncoder().encodeToString(content);
        base64Content = "data:" + contentType + ";base64," + base64Content;
        return base64Content;
    }

    /**
     * Envoie une réponse texte au client.
     *
     * @param clientSocket Le socket client.
     * @param contentType Le type de contenu.
     * @param cheminFichier Le chemin du fichier à envoyer.
     * @throws IOException En cas d'erreur d'entrée/sortie.
     */
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
                Path path = Paths.get("/var/www/" + image);
                String nouvelleImage = "";
                if (Files.exists(path)) {
                    byte[] content = Files.readAllBytes(path);
                    nouvelleImage = envoyerReponseBase64(clientSocket, contentType, content);
                }
                ligne = ligne.replace(image, nouvelleImage);
                codeHtml += ligne;
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
                String code = "";
                ligne = fichier.readLine();
                while (!ligne.contains("</code>")) {
                    code += ligne;
                    ligne = fichier.readLine();
                }
                ProcessBuilder processBuilder = new ProcessBuilder();
                List<String> commande = new ArrayList<>();
                commande.add(interpreteur);

                File tempFile = new File("/var/fichier_script");
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(code);
                }
                String chemin = tempFile.getAbsolutePath();
                commande.add("/var/fichier_script");
                processBuilder.command(commande);
                try {
                    Process process = processBuilder.start();
                    process.waitFor();
                    StringBuilder output = new StringBuilder();

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                    reader.close();

                    int exitVal = process.waitFor();
                    if (exitVal == 0) {
                        codeHtml += output + " ";
                    } else {
                        System.out.println("échec");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tempFile.delete();
            } else {
                codeHtml += ligne + " ";
            }
            ligne = fichier.readLine();
        }
        fichier.close();
        byte[] content = codeHtml.getBytes();
        String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + content.length + "\r\n\r\n";
        envoyerBytes(clientSocket, responseHeaders.getBytes());
        envoyerBytes(clientSocket, content);
    }

    /**
     * Envoie des bytes au client.
     *
     * @param clientSocket Le socket client.
     * @param bytes Les bytes à envoyer.
     * @throws IOException En cas d'erreur d'entrée/sortie.
     */
    private void envoyerBytes(Socket clientSocket, byte[] bytes) throws IOException {
        OutputStream out = clientSocket.getOutputStream();
        out.write(bytes);
        out.flush();
    }
}
