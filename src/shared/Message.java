package shared;

import java.io.Serial;
import java.io.Serializable;

public class Message implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private final MessageTypes type;
  private final String content;
  private final String HMAC;

  public Message(MessageTypes type, String content, String hMAC) {
    this.type = type;
    this.content = content;
    this.HMAC = hMAC;
  }

  public MessageTypes getType() {
    return type;
  }

  public String getHMAC() {
    return HMAC;
  }

  public String getContent() {
    return content;
  }
}
