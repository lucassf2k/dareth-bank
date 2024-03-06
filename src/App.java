import shared.crypto.AES128;
import shared.crypto.HMAC;

public class App {
    public static void main(String[] args) throws Exception {
        final var message = "mensagem confidencial";
        // final var key = "chave12346";
        // final var messageHMAC = HMAC.hMac(key, message);
        // System.out.println("HMAC: " + messageHMAC);
        // System.out.println("Tamanho: " + messageHMAC.getBytes().length);
        final var key = AES128.generateKeys();
        final var encryptedMessage = AES128.encrypt(message, key);
        System.out.println("AES: " + encryptedMessage);
        final var decryptedMessage = AES128.decrypt(encryptedMessage, key);
        System.out.println("Mensagem: " + decryptedMessage);
    }
}
