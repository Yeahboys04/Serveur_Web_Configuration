import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class VerificateurAdresseIP {

    public static boolean estDansReseau(String adresseIP, String adresseReseau, int prefixe) throws UnknownHostException {
        byte[] ipOctets = InetAddress.getByName(adresseIP).getAddress();
        byte[] reseauOctets = InetAddress.getByName(adresseReseau).getAddress();
        byte[] masqueOctets = creerMasque(prefixe, ipOctets.length * 8);

        if (ipOctets.length != reseauOctets.length) {
            return false; // Les longueurs des adresses ne correspondent pas
        }

        byte[] ipMasquee = appliquerMasque(ipOctets, masqueOctets);
        byte[] reseauMasquee = appliquerMasque(reseauOctets, masqueOctets);

        return Arrays.equals(ipMasquee, reseauMasquee);
    }

    private static byte[] appliquerMasque(byte[] adresse, byte[] masque) {
        byte[] resultat = new byte[adresse.length];
        for (int i = 0; i < adresse.length; i++) {
            resultat[i] = (byte) (adresse[i] & masque[i]);
        }
        return resultat;
    }

    private static byte[] creerMasque(int prefixe, int tailleBits) {
        byte[] masque = new byte[tailleBits / 8];
        int nombreOctetsComplet = prefixe / 8;
        int bitsRestants = prefixe % 8;

        for (int i = 0; i < nombreOctetsComplet; i++) {
            masque[i] = (byte) 255;
        }
        if (bitsRestants > 0) {
            masque[nombreOctetsComplet] = (byte) (255 << (8 - bitsRestants));
        }

        return masque;
    }
}