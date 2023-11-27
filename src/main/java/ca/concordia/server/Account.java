package ca.concordia.server;

import java.util.concurrent.atomic.AtomicInteger;
public class Account {
    private final AtomicInteger balance; // Atomic integer to ensure thread safety for balance operations
    private final int id; // Unique identifier for the account

    // Constructor that initializes the account based on a line from an accounts file
    public Account(String line) {
        String[] parts = line.split(",");
        if (parts.length == 2) {
            this.id = Integer.parseInt(parts[0].trim());
            this.balance = new AtomicInteger(Integer.parseInt(parts[1].trim()));
        } else {
            // Throw an exception if the input line is not in the expected format
            throw new IllegalArgumentException("Invalid line in accounts file: " + line);
        }
    }

    // Getter method to retrieve the current balance of the account
    public int getBalance() {
        return balance.get();
    }

    // Getter method to retrieve the unique identifier of the account
    public int getId() {
        return id;
    }

    // Method to perform a withdrawal from the account with the specified amount
    public void withdraw(int amount) {
        int currentBalance = balance.get();
        if (currentBalance >= amount) {
            // Using compareAndSet to ensure atomic withdrawal
            while (!balance.compareAndSet(currentBalance, currentBalance - amount)) {
                // Retry the withdrawal if the balance has changed during the operation
                currentBalance = balance.get();
            }
        } else {
            // Throw an exception if there are insufficient funds for the withdrawal
            throw new IllegalArgumentException("Insufficient funds");
        }
    }

    // Method to perform a deposit into the account with the specified amount
    public void deposit(int amount) {
        // Using addAndGet to ensure atomic deposit
        balance.addAndGet(amount);
    }

    // Override toString method to provide a string representation of the account
    @Override
    public String toString() {
        return "Account{id=" + id + ", balance=" + balance + '}';
    }
}