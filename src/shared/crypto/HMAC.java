package shared.crypto;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HMAC {
  private static final String ALG = "HmacSHA256";

  public static String hMac(final String key, final String message) throws NoSuchAlgorithmException,
      UnsupportedEncodingException, InvalidKeyException {
    final var shaMAC = Mac.getInstance(HMAC.ALG);
    final var keyMAC = new SecretKeySpec(key.getBytes("UTF-8"), HMAC.ALG);
    shaMAC.init(keyMAC);
    final var bytesHMAC = shaMAC.doFinal(message.getBytes("UTF-8"));
    return HMAC.byte2Hex(bytesHMAC);
  }

  private static String byte2Hex(final byte[] bytes) {
    final var sb = new StringBuilder();
    for (var b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
