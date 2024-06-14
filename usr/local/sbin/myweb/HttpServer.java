import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpServer {
    private ServerSocket serverSocket;
    private String cheminVersConfig;
    private GestionDesAccess access;
    private GestionDesErrors error;
    private DemandeConnection demande;

    public HttpServer(){
    }
    public HttpServer(int port, String cheminVersConfig, String cheminEcritureLogAccess, String cheminEcritureLogErrors, List<String> ipAccepter, List<String> ipRefuser) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.cheminVersConfig = cheminVersConfig;
        this.access = new GestionDesAccess(cheminEcritureLogAccess);
        this.error = new GestionDesErrors(cheminEcritureLogErrors);
        this.demande = new DemandeConnection(cheminVersConfig, ipAccepter, ipRefuser);
    }

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

    public static List<String> genererListIP(String a){
        String [] r = a.split(",");
        List<String> s = new ArrayList<String>();
        for(int i = 0 ;i<r.length;i++){
               s.add(r[i]);
         }
        return s;
    }

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
        if(serverSocket == null){
            this.serverSocket = new ServerSocket(port);
        }
        this.cheminVersConfig = cheminFichierConf;
        this.access = new GestionDesAccess(cheminAccesLog);
        this.error = new GestionDesErrors(cheminErrorLog);
        this.demande = new DemandeConnection(cheminFichierConf, ipAccepter, ipRejeter);
    }
}
