import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe HttpServer qui représente un serveur HTTP capable de gérer les connexions des clients,
 * de lire une configuration XML et de traiter les requêtes HTTP.
 */
public class HttpServer {
    private ServerSocket serverSocket;
    private String cheminVersConfig;
    private GestionDesAccess access;
    private GestionDesErrors error;
    private DemandeConnection demande;

    /**
     * Constructeur par défaut.
     */
    public HttpServer() {
    }

    /**
     * Constructeur pour initialiser le serveur avec les paramètres spécifiés.
     *
     * @param port Le port sur lequel le serveur écoute.
     * @param cheminVersConfig Chemin vers le fichier de configuration.
     * @param cheminEcritureLogAccess Chemin vers le fichier de journal d'accès.
     * @param cheminEcritureLogErrors Chemin vers le fichier de journal des erreurs.
     * @param ipAccepter Liste des adresses IP acceptées.
     * @param ipRefuser Liste des adresses IP refusées.
     * @throws IOException En cas d'erreur d'entrée/sortie.
     */
    public HttpServer(int port, String cheminVersConfig, String cheminEcritureLogAccess, String cheminEcritureLogErrors, List<String> ipAccepter, List<String> ipRefuser) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.cheminVersConfig = cheminVersConfig;
        this.access = new GestionDesAccess(cheminEcritureLogAccess);
        this.error = new GestionDesErrors(cheminEcritureLogErrors);
        this.demande = new DemandeConnection(cheminVersConfig, ipAccepter, ipRefuser);
    }

    /**
     * Méthode pour démarrer le serveur et accepter les connexions des clients.
     */
    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                demande.handleRequest(clientSocket, access, error);
                this.init();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Génère une liste d'adresses IP à partir d'une chaîne de caractères.
     *
     * @param a La chaîne de caractères contenant les adresses IP séparées par des virgules.
     * @return La liste des adresses IP.
     */
    public static List<String> genererListIP(String a) {
        String[] r = a.split(",");
        List<String> s = new ArrayList<>();
        for (String value : r) {
            s.add(value);
        }
        return s;
    }

    /**
     * Méthode principale pour lancer le serveur.
     *
     * @param args Arguments de la ligne de commande.
     */
    public static void main(String[] args) {
        try {
            // Chargement du fichier de configuration
            HttpServer serveur = new HttpServer();
            serveur.init();
            serveur.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialise le serveur en lisant les valeurs de configuration à partir d'un fichier XML.
     *
     * @throws IOException En cas d'erreur d'entrée/sortie.
     * @throws SAXException En cas d'erreur de parsing XML.
     * @throws ParserConfigurationException En cas d'erreur de configuration du parser.
     */
    private void init() throws IOException, SAXException, ParserConfigurationException {
        File xmlFile = new File("etc/myweb/myweb.conf");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        // Lire les valeurs de configuration
        int port = Integer.parseInt(doc.getElementsByTagName("port").item(0).getTextContent());
        String cheminFichierConf = doc.getElementsByTagName("root").item(0).getTextContent();
        String cheminAccesLog = doc.getElementsByTagName("accesslog").item(0).getTextContent();
        String cheminErrorLog = doc.getElementsByTagName("errorlog").item(0).getTextContent();
        // La liste des IP acceptées et rejetées
        List<String> ipAccepter = genererListIP(doc.getElementsByTagName("accept").item(0).getTextContent());
        List<String> ipRejeter = genererListIP(doc.getElementsByTagName("reject").item(0).getTextContent());

        // Initialiser le serveur socket si ce n'est pas déjà fait
        if (serverSocket == null) {
            this.serverSocket = new ServerSocket(port);
        } else {
            if (port != serverSocket.getLocalPort()) {
                this.serverSocket = new ServerSocket(port);
            }
        }
        this.cheminVersConfig = cheminFichierConf;
        this.access = new GestionDesAccess(cheminAccesLog);
        this.error = new GestionDesErrors(cheminErrorLog);
        this.demande = new DemandeConnection(cheminFichierConf, ipAccepter, ipRejeter);
    }
}
