import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;

public class ConfigLoader {
    private int port = 80;
    private String root = ".";
    private String accessLog = null;
    private String errorLog = null;
    private List<String> acceptedIPs = new ArrayList<>();
    private List<String> rejectedIPs = new ArrayList<>();

    public ConfigLoader(String configFilePath) {
        try {
            File file = new File(configFilePath);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("webconf");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    port = Integer.parseInt(getTagValue("port", element, "80"));
                    root = getTagValue("root", element, ".");
                    accessLog = getTagValue("accesslog", element, null);
                    errorLog = getTagValue("errorlog", element, null);
                    parseIPs(element, "accept", acceptedIPs);
                    parseIPs(element, "reject", rejectedIPs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTagValue(String tag, Element element, String defaultValue) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return defaultValue;
    }

    private void parseIPs(Element element, String tag, List<String> ipList) {
        NodeList nodeList = element.getElementsByTagName(tag);
        for (int i = 0; i < nodeList.getLength(); i++) {
            ipList.add(nodeList.item(i).getTextContent());
        }
    }

    public int getPort() { return port; }
    public String getRoot() { return root; }
    public String getAccessLog() { return accessLog; }
    public String getErrorLog() { return errorLog; }
    public List<String> getAcceptedIPs() { return acceptedIPs; }
    public List<String> getRejectedIPs() { return rejectedIPs; }
}
