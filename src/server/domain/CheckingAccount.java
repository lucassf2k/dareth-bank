package server.domain;

public class CheckingAccount {
  private final String CPF;
  private final String customerName;
  private final String address;
  private final String phoneNumber;
  private final String accountNumber;
  private final String password;
  private double balance;

  public CheckingAccount(
      final String CPF, final String customerName, final String address,
      final String phoneNumber, final String accountNumber, final String password) {
    this.CPF = CPF;
    this.customerName = customerName;
    this.address = address;
    this.phoneNumber = phoneNumber;
    this.accountNumber = accountNumber;
    this.password = password;
    this.balance = 0.0;
  }

  public void deposit(final double value) {
    if (value <= 0) {
      System.out.println("valor insuficiente para depósito.");
      return;
    }
    this.balance += value;
  }

  public void withdraw(final double value) {
    if (!canWithdraw(value, this.balance))
      return;
    this.balance -= value;
  }

  public void transfer(final CheckingAccount recipient, final double value) {
    if (!canTransfer(value)) {
      System.out.println("saldo insuficiente");
      return;
    }
    withdraw(value);
    recipient.deposit(value);
  }

  public void simulateInvestiment(
      final String type, final CheckingAccount checkingAccount, final double value) {
    if (type.equalsIgnoreCase("savings")) {
      double balance = checkingAccount.getBalance();
      for (var month = 1; month <= 12; month++) {
        balance += (balance * 0.005);
      }
      System.out.println("Na poupança em 1 ano: " + balance);
      return;
    }
    double balanceSixMonths = 0.0;
    double balanceTwelveMonths = 0.0;
    double balance = checkingAccount.getBalance();
    for (var month = 1; month <= 12; month++) {
      balance += (value * 0.015);
      if (month == 6)
        balanceSixMonths = balance;
      if (month == 12)
        balanceTwelveMonths = balance;
    }

    System.out.println("Em 6 meses: " + balanceSixMonths);
    System.out.println("Em 1 ano: " + balanceTwelveMonths);
  }

  private boolean canTransfer(final double value) {
    if (this.balance >= value)
      return Boolean.TRUE;
    return Boolean.FALSE;
  }

  private boolean canWithdraw(final double value, final double balance) {
    if (value <= 0.0) {
      System.out.println("valor insuficiente");
      return Boolean.FALSE;
    }
    if (balance < value) {
      System.out.println("saldo insuficiente");
      return Boolean.FALSE;
    }
    return Boolean.TRUE;
  }

  public String getCPF() {
    return CPF;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getAddress() {
    return address;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public double getBalance() {
    return balance;
  }

  public void setBalance(double balance) {
    this.balance = balance;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public String getPassword() {
    return password;
  }
}
