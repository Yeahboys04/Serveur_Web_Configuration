import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpServer {
    private ServerSocket serverSocket;
    private String cheminVersConfig;
    private GestionDesAccess access;
    private GestionDesErrors error;
    private DemandeConnection demande;

    public HttpServer(int port, String CheminVersConfig, String CheminEcritureLogAccess, String CHeminEcritureLogErrors, List<String> IpAccepter, List<String> IpRefuser) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.cheminVersConfig = CheminVersConfig;
        this.access = new GestionDesAccess(CheminEcritureLogAccess);
        this.error = new GestionDesErrors(CHeminEcritureLogErrors);
        this.demande = new DemandeConnection(CheminVersConfig, IpAccepter, IpRefuser);
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                demande.handleRequest(clientSocket, access, error);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Chargement du fichier de config
            File xmlFile = new File("config/myweb.conf");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Lire les valeurs de configuration
            int port = Integer.parseInt(doc.getElementsByTagName("port").item(0).getTextContent());
            String cheminFichierConf = doc.getElementsByTagName("root").item(0).getTextContent();
            String cheminAccesLog = doc.getElementsByTagName("accesslog").item(0).getTextContent();
            String cheminErrorLog = doc.getElementsByTagName("errorlog").item(0).getTextContent();
            //La liste des Ip accepter et rejeter doit etre de la forme <accept>Ip,Ip,...</accept> dans le fichier conf
            List<String> IpAccepter = Stream.of(doc.getElementsByTagName("accept").item(0).getTextContent().split(","))
                    .collect(Collectors.toList());
            //Meme chose pour les rejeter
            List<String> IpRejeter = Stream.of(doc.getElementsByTagName("reject").item(0).getTextContent().split(","))
                    .collect(Collectors.toList());

            // Initialisation du serveur
            HttpServer server = new HttpServer(port, cheminFichierConf, cheminAccesLog, cheminErrorLog, IpAccepter, IpRejeter);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
