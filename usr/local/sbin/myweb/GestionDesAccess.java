import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Classe GestionDesAccess pour gérer les journaux d'accès.
 */
public class GestionDesAccess {
    private String accessLogPath;

    /**
     * Constructeur pour initialiser le chemin du fichier de journal d'accès.
     *
     * @param accessLogPath Chemin du fichier de journal d'accès.
     */
    public GestionDesAccess(String accessLogPath) {
        this.accessLogPath = accessLogPath;
    }

    /**
     * Méthode pour enregistrer un message dans le fichier de journal d'accès.
     *
     * @param message Le message à enregistrer.
     */
    public void logAccess(String message) {
        try (FileWriter fw = new FileWriter(accessLogPath, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            // Écriture du message dans le fichier de log
            out.println(message);
        } catch (IOException e) {
            // En cas d'erreur d'entrée/sortie, imprimer la trace de la pile d'appels
            e.printStackTrace();
        }
    }
}
