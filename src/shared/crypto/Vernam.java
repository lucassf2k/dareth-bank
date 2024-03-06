package shared.crypto;

public class Vernam {
  private Vernam() {
  }

  public static String encrypt(final String message, final String key) {
    final var result = new StringBuilder();
    for (var i = 0; i < message.length(); i++) {
      final var caractere = message.charAt(i);
      final var keyChar = key.charAt(i % key.length());
      final var encrypted = (char) (caractere ^ keyChar);
      result.append(encrypted);
    }
    return result.toString();
  }

  public static String decrypt(final String messageEncryped, final String key) {
    return Vernam.encrypt(messageEncryped, key);
  }
}
