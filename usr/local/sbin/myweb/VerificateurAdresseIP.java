import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Classe utilitaire pour vérifier si une adresse IP appartient à un réseau donné.
 */
public class VerificateurAdresseIP {

    /**
     * Vérifie si une adresse IP appartient à un réseau donné.
     *
     * @param adresseIP L'adresse IP à vérifier.
     * @param adresseReseau L'adresse réseau.
     * @param prefixe Le préfixe du masque réseau (en bits).
     * @return true si l'adresse IP appartient au réseau, false sinon.
     * @throws UnknownHostException En cas d'erreur de résolution de l'adresse IP.
     */
    public static boolean estDansReseau(String adresseIP, String adresseReseau, int prefixe) throws UnknownHostException {
        // Convertit les adresses IP et réseau en tableaux d'octets
        byte[] ipOctets = InetAddress.getByName(adresseIP).getAddress();
        byte[] reseauOctets = InetAddress.getByName(adresseReseau).getAddress();
        // Crée le masque de réseau
        byte[] masqueOctets = creerMasque(prefixe, ipOctets.length * 8);

        // Vérifie si les longueurs des adresses IP et réseau correspondent
        if (ipOctets.length != reseauOctets.length) {
            return false; // Les longueurs des adresses ne correspondent pas
        }

        // Applique le masque aux adresses IP et réseau
        byte[] ipMasquee = appliquerMasque(ipOctets, masqueOctets);
        byte[] reseauMasquee = appliquerMasque(reseauOctets, masqueOctets);

        // Compare les adresses IP masquées
        return Arrays.equals(ipMasquee, reseauMasquee);
    }

    /**
     * Applique un masque à une adresse IP.
     *
     * @param adresse L'adresse IP sous forme de tableau d'octets.
     * @param masque Le masque sous forme de tableau d'octets.
     * @return L'adresse IP masquée.
     */
    private static byte[] appliquerMasque(byte[] adresse, byte[] masque) {
        byte[] resultat = new byte[adresse.length];
        for (int i = 0; i < adresse.length; i++) {
            resultat[i] = (byte) (adresse[i] & masque[i]);
        }
        return resultat;
    }

    /**
     * Crée un masque de réseau à partir d'un préfixe.
     *
     * @param prefixe Le préfixe du masque réseau (en bits).
     * @param tailleBits La taille totale en bits de l'adresse IP (32 pour IPv4, 128 pour IPv6).
     * @return Le masque réseau sous forme de tableau d'octets.
     */
    private static byte[] creerMasque(int prefixe, int tailleBits) {
        byte[] masque = new byte[tailleBits / 8];
        int nombreOctetsComplet = prefixe / 8;
        int bitsRestants = prefixe % 8;

        // Remplit les octets complets avec des bits à 1
        for (int i = 0; i < nombreOctetsComplet; i++) {
            masque[i] = (byte) 255;
        }
        // Remplit l'octet partiellement complet
        if (bitsRestants > 0) {
            masque[nombreOctetsComplet] = (byte) (255 << (8 - bitsRestants));
        }

        return masque;
    }
}
