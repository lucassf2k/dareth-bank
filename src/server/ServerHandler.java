package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import server.domain.Bank;
import server.domain.CheckingAccount;
import shared.FileService;
import shared.Message;
import shared.MessageTypes;
import shared.crypto.AES128;
import shared.crypto.HMAC;
import shared.crypto.Vernam;

public class ServerHandler implements Runnable {
  private final Socket client;
  private static ObjectOutputStream output;
  private static ObjectInputStream input;
  private final Bank bank;
  private int response = 1;
  private CheckingAccount currentCheckingAccount = null;

  public ServerHandler(Socket client) {
    this.client = client;
    this.bank = new Bank();
  }

  @Override
  public void run() {
    System.out.println("conectando com o cliente...");
    try {
      output = new ObjectOutputStream(client.getOutputStream());
      input = new ObjectInputStream(client.getInputStream());
      boolean hasConnection = Boolean.TRUE;
      while (hasConnection) {
        final var request = (Message) input.readObject();
        if (request.getType().equals(MessageTypes.LOGIN)) {
          authentication(request);
          continue;
        }
        if (request.getType().equals(MessageTypes.WITHDRAW)) {
          withdraw(request);
          continue;
        }
        if (request.getType().equals(MessageTypes.GET_BALANCE)) {
          getBalance();
          continue;
        }
        if (request.getType().equals(MessageTypes.DEPOSIT)) {
          deposit(request);
          continue;
        }
      }
      input.close();
      output.close();
      client.close();
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private void sendMessage(final int response) {
    try {
      output.writeInt(response);
      output.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void authentication(final Message request) {
    System.out.println("autenticando...");
    final var isAuthenticated = checkMessageAuthenticity(request.getContent(), request.getHMAC());
    System.out.println("descriptando mensagem...");
    final var vernamMessageDecrypted = decrypt(request);
    final var splitedMessage = vernamMessageDecrypted.split("-");
    final var accountNumber = splitedMessage[0];
    final var password = splitedMessage[1];
    final var isRegistered = bank.login(accountNumber, password);
    if (isAuthenticated && isRegistered) {
      response = 2;
      currentCheckingAccount = bank.getCheckingAccount(accountNumber);
    }
    sendMessage(response);
  }

  private void withdraw(final Message request) {
    System.out.println("descriptografando a mensagem...");
    final var isAuthenticated = checkMessageAuthenticity(request.getContent(), request.getHMAC());
    if (!isAuthenticated) {
      response = 1;
    }
    final var decryptedMessage = decrypt(request);
    currentCheckingAccount.withdraw(Double.valueOf(decryptedMessage));
    try {
      output.writeUTF("seu saldo é de " + currentCheckingAccount.getBalance());
      output.writeInt(response);
      output.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void getBalance() {
    try {
      output.writeUTF("seu saldo é de " + currentCheckingAccount.getBalance());
      output.writeInt(response);
      output.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void deposit(final Message request) {
    System.out.println("verificando se é uma mensagem autentica...");
    final var isAuthenticated = checkMessageAuthenticity(request.getContent(), request.getHMAC());
    if (!isAuthenticated) {
      response = 1;
    }
    final var decryptedMessage = decrypt(request);
    currentCheckingAccount.deposit(Double.valueOf(decryptedMessage));
    try {
      output.writeUTF("seu saldo é de " + currentCheckingAccount.getBalance());
      output.writeInt(response);
      output.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String decrypt(final Message request) {
    final var keys = getKeys();
    String decryptedMessage;
    try {
      final var AESMessageDecrypted = AES128.decrypt(request.getContent(), AES128.stringToSecretKey(keys[2]));
      decryptedMessage = Vernam.decrypt(AESMessageDecrypted, keys[0]);
    } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
        | BadPaddingException e) {
      throw new RuntimeException(e);
    }
    return decryptedMessage;
  }

  private String[] getKeys() {
    final var filename = "keys.txt";
    final var keys = FileService.get(filename);
    final var splitedKeys = keys.split("\n");
    final var vernamKey = splitedKeys[0];
    final var HMACKey = splitedKeys[1];
    final var AESKey = splitedKeys[2];
    final var output = new String[splitedKeys.length];
    output[0] = vernamKey;
    output[1] = HMACKey;
    output[2] = AESKey;
    return output;
  }

  private boolean checkMessageAuthenticity(final String encryptedMessage, final String HMACMessage) {
    try {
      final var keys = getKeys();
      final var HMACCompare = HMAC.hMac(keys[1], encryptedMessage);
      if (HMACMessage.equals(HMACCompare)) {
        return Boolean.TRUE;
      }
      return Boolean.FALSE;
    } catch (InvalidKeyException | NoSuchAlgorithmException
        | UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
