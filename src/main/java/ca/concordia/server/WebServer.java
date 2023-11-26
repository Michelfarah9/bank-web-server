package ca.concordia.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WebServer {
    private final ExecutorService threadPool = Executors.newFixedThreadPool(1000); // Adjust the pool size as needed
     private static final Map<Integer, Account> accounts = new ConcurrentHashMap<>();
    private int count = 0;
    private final Lock transferLock = new ReentrantLock();

private void initializeAccountsFromFile(String filePath) {
        try {
            List<String> lines = Files.readAllLines(Path.of(filePath));
            for (String line : lines) {
                Account account = new Account(line);
                accounts.put(account.getId(), account);
            }
        } catch (IOException e) {
            e.printStackTrace();  // Handle the exception appropriately in your application
        }
    }
    private static void sendError(OutputStream out, String errorMessage) throws IOException {
        String response = "HTTP/1.1 400 Bad Request\r\n" +
                          "Content-Length: " + errorMessage.length() + "\r\n" +
                          "Content-Type: text/plain\r\n\r\n" +
                          errorMessage;
        out.write(response.getBytes());
        out.flush();
    }

    private static void sendSuccess(OutputStream out, String successMessage) throws IOException {
        String response = "HTTP/1.1 200 OK\r\n" +
                          "Content-Length: " + successMessage.length() + "\r\n" +
                          "Content-Type: text/plain\r\n\r\n" +
                          successMessage;
        out.write(response.getBytes());
        out.flush();
    }
    



    
    private void printBalances() {
        System.out.println("Current Balances:");
        for (Account account : accounts.values()) {
            System.out.println(account);
        }
        System.out.println();
    }
    
    

    private void processTransfer(String sourceAccount, int sourceValue, String destAccount, int destValue, OutputStream out) throws IOException {
        transferLock.lock();
        try {
            // Convert account numbers to integers
            int sourceAccountId = Integer.parseInt(sourceAccount);
            int destAccountId = Integer.parseInt(destAccount);
    
            // Get source and destination accounts
            Account source = accounts.get(sourceAccountId);
            Account dest = accounts.get(destAccountId);
    
            // Check if accounts exist
            if (source == null || dest == null) {
                sendError(out, "One or more accounts do not exist");
                return;
            }
    
            // Print current balances before transfer
            System.out.println("Before Transfer:");
            printBalances();
    
            // Check if there are sufficient funds in the source account
            if (source.getBalance() < sourceValue) {
                sendError(out, "Insufficient funds in the source account");
                return;
            }
    
            // Perform the fund transfer
            source.withdraw(sourceValue);
            dest.deposit(destValue);
    
            // Print updated balances after transfer
            System.out.println("After Transfer:");
            printBalances();
    
            // Send success response
            sendSuccess(out, "Transfer successful");
        } catch (NumberFormatException e) {
            sendError(out, "Invalid account number format");
        } finally {
            transferLock.unlock();
        }
    }
    
    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
    
        try {
            while (true) {
                count++;
                System.out.println("Waiting for client " + count);
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client...");
    
                // Use the thread pool to handle the request
                threadPool.submit(() -> handleRequest(clientSocket));
            }
        } finally {
            serverSocket.close(); // Close the server socket in a finally block
        }
    }

    private void handleRequest(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();

            String request = in.readLine();
            if (request != null) {
                if (request.startsWith("GET")) {
                    // Handle GET request
                    handleGetRequest(out,request);
                } else if (request.startsWith("POST")) {
                    // Handle POST request
                    handlePostRequest(in, out);
                }
            }

            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleGetRequest(OutputStream out, String request) throws IOException {
        if (request.startsWith("GET /balances")) {
            // Handle request to display balances
            handleBalancesRequest(out);
        } else if (request.startsWith("GET")) {
            // Handle other GET requests
            handleDefaultGetRequest(out);
        }
    }

    private static void handleDefaultGetRequest(OutputStream out) throws IOException {
        // Respond with a basic HTML page
        System.out.println("Handling GET request");
        String response = "HTTP/1.1 200 OK\r\n\r\n" +
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<title>Concordia Transfers</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "<h1>Welcome to Concordia Transfers</h1>\n" +
                "<p>Select the account and amount to transfer</p>\n" +
                "\n" +
                "<form action=\"/submit\" method=\"post\">\n" +
                "        <label for=\"account\">Account:</label>\n" +
                "        <input type=\"text\" id=\"account\" name=\"account\"><br><br>\n" +
                "\n" +
                "        <label for=\"value\">Value:</label>\n" +
                "        <input type=\"text\" id=\"value\" name=\"value\"><br><br>\n" +
                "\n" +
                "        <label for=\"toAccount\">To Account:</label>\n" +
                "        <input type=\"text\" id=\"toAccount\" name=\"toAccount\"><br><br>\n" +
                "\n" +
                "        <label for=\"toValue\">To Value:</label>\n" +
                "        <input type=\"text\" id=\"toValue\" name=\"toValue\"><br><br>\n" +
                "\n" +
                "        <input type=\"submit\" value=\"Submit\">\n" +
                "    </form>\n" +
                "</body>\n" +
                "</html>\n";
        out.write(response.getBytes());
        out.flush();
    }

    private static void handleBalancesRequest(OutputStream out) throws IOException {
        // Respond with HTML displaying current balances
        StringBuilder response = new StringBuilder("HTTP/1.1 200 OK\r\n\r\n" +
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<title>Account Balances</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "<h1>Current Account Balances</h1>\n" +
                "<ul>");
    
        // Iterate over accounts and display balances
        for (Account account : accounts.values()) {
            response.append("<li>").append(account.toString()).append("</li>");
        }
    
        response.append("</ul>\n</body>\n</html>");
    
        out.write(response.toString().getBytes());
        out.flush();
    }

    private void handlePostRequest(BufferedReader in, OutputStream out) throws IOException {
        System.out.println("Handling post request");
        StringBuilder requestBody = new StringBuilder();
        int contentLength = 0;
        String line;
    
        // Read headers to get content length
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Content-Length")) {
                contentLength = Integer.parseInt(line.substring(line.indexOf(' ') + 1));
            }
        }
    
        // Read the request body based on content length
        for (int i = 0; i < contentLength; i++) {
            requestBody.append((char) in.read());
        }
    
        System.out.println(requestBody.toString());
        // Parse the request body as URL-encoded parameters
        String[] params = requestBody.toString().split("&");
        String account = null, value = null, toAccount = null, toValue = null;
    
        for (String param : params) {
            String[] parts = param.split("=");
            if (parts.length == 2) {
                String key = URLDecoder.decode(parts[0], "UTF-8");
                String val = URLDecoder.decode(parts[1], "UTF-8");
    
                switch (key) {
                    case "account":
                        account = val;
                        break;
                    case "value":
                        value = val;
                        break;
                    case "toAccount":
                        toAccount = val;
                        break;
                    case "toValue":
                        toValue = val;
                        break;
                }
            }
        }
        System.out.println("Before Transfer:");
        printBalances();
        // Call the processTransfer method
        processTransfer(account, Integer.parseInt(value), toAccount, Integer.parseInt(toValue), out);
        System.out.println("After Transfer:");
        printBalances();
        // Create the response
        String responseContent = "<html><body><h1>Thank you for using Concordia Transfers</h1>" +
                "<h2>Received Form Inputs:</h2>"+
                "<p>Account: " + account + "</p>" +
                "<p>Value: " + value + "</p>" +
                "<p>To Account: " + toAccount + "</p>" +
                "<p>To Value: " + toValue + "</p>" +
                "</body></html>";
    
        // Respond with the received form inputs
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + responseContent.length() + "\r\n" +
                "Content-Type: text/html\r\n\r\n" +
                responseContent;
    
        out.write(response.getBytes());
        out.flush();
    }
    

    public static void main(String[] args) {
        WebServer server = new WebServer();
    
        // Initialize accounts from the file (replace "path/to/accounts.txt" with the actual path)
        server.initializeAccountsFromFile("C:/Users/pierh/OneDrive/Desktop/COEN 346 assignments/assignment 2/webserver/webserver/webserver/AccountBalance.txt");
    
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}

