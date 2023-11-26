package ca.concordia.server;

import java.util.concurrent.atomic.AtomicInteger;
public class Account {
    private final AtomicInteger balance;
    private final int id;

    public Account(String line) {
        String[] parts = line.split(",");
        if (parts.length == 2) {
            this.id = Integer.parseInt(parts[0].trim());
            this.balance = new AtomicInteger(Integer.parseInt(parts[1].trim()));
        } else {
            throw new IllegalArgumentException("Invalid line in accounts file: " + line);
        }
    }

    public int getBalance() {
        return balance.get();
    }

    public int getId(){
        return id;
    }

    public void withdraw(int amount) {
        int currentBalance = balance.get();
        if (currentBalance >= amount) {
            // Using compareAndSet to ensure atomic withdrawal
            while (!balance.compareAndSet(currentBalance, currentBalance - amount)) {
                currentBalance = balance.get();
            }
        } else {
            throw new IllegalArgumentException("Insufficient funds");
        }
    }

    public void deposit(int amount) {
        // Using addAndGet to ensure atomic deposit
        balance.addAndGet(amount);
    }

    @Override
    public String toString() {
        return "Account{id=" + id + ", balance=" + balance + '}';
    }
}
