import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Classe GestionDesErrors pour gérer les journaux d'erreurs.
 */
public class GestionDesErrors {
    private String errorLogPath;

    /**
     * Constructeur pour initialiser le chemin du fichier de journal d'erreurs.
     *
     * @param errorLogPath Chemin du fichier de journal d'erreurs.
     */
    public GestionDesErrors(String errorLogPath) {
        this.errorLogPath = errorLogPath;
    }

    /**
     * Méthode pour enregistrer un message d'erreur dans le fichier de journal d'erreurs.
     *
     * @param message Le message d'erreur à enregistrer.
     */
    public void logError(String message) {
        try (FileWriter fw = new FileWriter(errorLogPath, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            // Écriture du message dans le fichier de log des erreurs
            out.println(message);
        } catch (IOException e) {
            // En cas d'erreur d'entrée/sortie, imprimer la trace de la pile d'appels
            e.printStackTrace();
        }
    }
}
