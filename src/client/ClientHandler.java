package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import shared.FileService;
import shared.Message;
import shared.MessageTypes;
import shared.crypto.AES128;
import shared.crypto.HMAC;
import shared.crypto.Keys;
import shared.crypto.Vernam;

public class ClientHandler implements Runnable {
  private int hasRequest = 1;
  private final Socket client;
  private ObjectInputStream input;
  private ObjectOutputStream output;
  private String authenticationKey;
  private final Scanner scan = new Scanner(System.in);
  public static int PORT;

  public ClientHandler(final Socket client) {
    this.client = Objects.requireNonNull(client, "O socket do cliente não pode ser nulo.");
    PORT = this.client.getPort();
  }

  @Override
  public void run() {
    System.out.println("cliente conectou ao servidor.");
    try {
      output = new ObjectOutputStream(client.getOutputStream());
      input = new ObjectInputStream(client.getInputStream());
      boolean hasConnection = Boolean.TRUE;
      while (hasConnection) {
        welcome();
        while (hasRequest != 0) {
          switch (hasRequest) {
            case 1:
              login();
              break;
            case 2:
              menu();
              break;
            default:
              break;
          }
        }
      }
      output.close();
      client.close();
      System.out.println("cliente finalizando conexão...");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String makeMessageSecure(final String message, final String vernamKey, final String AESKey) {
    try {
      final var vernamMessage = Vernam.encrypt(message, vernamKey);
      final var AESMessage = AES128.encrypt(vernamMessage, AES128.stringToSecretKey(AESKey));
      return AESMessage;
    } catch (
        InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
        | IllegalBlockSizeException | BadPaddingException e) {
      throw new RuntimeException(e);
    }
  }

  private void sendMessage(final Message request) {
    try {
      output.writeObject(request);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void welcome() {
    System.out.println("==== Bem vindo ao Dareth Bank ====");
    generateKeysFile();
  }

  private void generateKeysFile() {
    final var filename = "keys.txt";
    FileService.insert(Keys.generateRandomBytes(20), filename);
    FileService.insert(Keys.generateRandomBytes(20), filename);
    final var keyAESString = Base64.getEncoder().encodeToString(Keys.generateAESKey().getEncoded());
    FileService.insert(keyAESString, filename);
  }

  private void login() {
    System.out.println("== LOGIN == ");
    System.out.print("- Número: ");
    final var accountNumber = scan.nextLine();
    System.out.print("- Senha: ");
    final var password = scan.nextLine();
    final var message = accountNumber + "-" + password;
    final var keys = getKeys();
    final var secureMessage = makeMessageSecure(message, keys[0], keys[2]);
    try {
      final var HMACMessage = HMAC.hMac(keys[1], secureMessage);
      final var request = new Message(MessageTypes.LOGIN, secureMessage, HMACMessage, authenticationKey);
      sendMessage(request);
      cleanTerminal();
      authenticationKey = input.readUTF();
      hasRequest = input.readInt();
    } catch (IOException | InvalidKeyException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private String[] getKeys() {
    final var keys = FileService.get("keys.txt");
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

  private void menu() {
    System.out.println("== O que quer fazer? ==");
    System.out.println("[1] - Sacar");
    System.out.println("[2] - Saldo");
    System.out.println("[3] - Depositar");
    System.out.println("[4] - Transferir");
    System.out.println("[5] - Simular investimento");
    System.out.print("Opção: ");
    final var result = scan.nextInt();
    switch (result) {
      case 1:
        withdraw();
        break;
      case 2:
        getBalance();
        break;
      case 3:
        deposit();
        break;
      case 4:
        transfer();
        break;
      case 5:
        investment();
        break;
      default:
        break;
    }
  }

  private void withdraw() {
    System.out.print("Digite o valor que quer retirar: ");
    final var value = scan.nextDouble();
    final var keys = getKeys();
    final var secureMessage = makeMessageSecure(String.valueOf(value), keys[0], keys[2]);
    try {
      final var HMACMessage = HMAC.hMac(keys[1], secureMessage);
      final var request = new Message(MessageTypes.WITHDRAW, secureMessage, HMACMessage, authenticationKey);
      sendMessage(request);
      hasRequest = input.readInt();
      cleanTerminal();
    } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void getBalance() {
    final var request = new Message(MessageTypes.GET_BALANCE, null, null, authenticationKey);
    sendMessage(request);
    try {
      cleanTerminal();
      System.out.println(input.readUTF());
      hasRequest = input.readInt();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void deposit() {
    System.out.print("Digite o valor que deseja depositar: ");
    final var value = scan.nextDouble();
    final var keys = getKeys();
    final var secureMessage = makeMessageSecure(String.valueOf(value), keys[0], keys[2]);
    try {
      final var HMACMessage = HMAC.hMac(keys[1], secureMessage);
      final var request = new Message(MessageTypes.DEPOSIT, secureMessage, HMACMessage, authenticationKey);
      sendMessage(request);
      cleanTerminal();
      hasRequest = input.readInt();
    } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void transfer() {
    System.out.print("Número da conta para qual quer tranferir: ");
    final var accountNumberRecipient = scan.nextInt();
    System.out.print("Valor da transferência: ");
    final var value = scan.nextDouble();
    final var keys = getKeys();
    final var message = accountNumberRecipient + "-" + value;
    final var secureMessage = makeMessageSecure(message, keys[0], keys[2]);
    try {
      final var HMACMessage = HMAC.hMac(keys[1], secureMessage);
      final var request = new Message(
          MessageTypes.TRANSFER, secureMessage, HMACMessage, authenticationKey);
      sendMessage(request);
      cleanTerminal();
      System.out.println(input.readUTF());
      hasRequest = input.readInt();
    } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
      throw new RuntimeException(e);
    }

  }

  private void investment() {
    System.out.println("[1] - Poupança");
    System.out.println("[2] - Fixa");
    final var option = scan.nextInt();
    final var keys = getKeys();
    final var secureMessage = makeMessageSecure(String.valueOf(option), keys[0], keys[2]);
    try {
      final var HMACMessage = HMAC.hMac(keys[1], secureMessage);
      final var request = new Message(
          MessageTypes.INVESTMENT, secureMessage, HMACMessage, authenticationKey);
      sendMessage(request);
      cleanTerminal();
      if (option == 1 || option == 2) {
        System.out.println(input.readUTF());
        System.out.println(input.readUTF());
        System.out.println(input.readUTF());
        hasRequest = input.readInt();
        return;
      }
      System.out.println(input.readUTF());
      hasRequest = input.readInt();
    } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void cleanTerminal() {
    try {
      // Verificar se o sistema operacional é Windows
      if (System.getProperty("os.name").startsWith("Windows")) {
        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
      } else {
        // Para sistemas Unix-like (Linux, macOS, etc.)
        System.out.print("\033[H\033[2J");
        System.out.flush();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
