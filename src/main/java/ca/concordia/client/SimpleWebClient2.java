package ca.concordia.client;

import java.io.*;
import java.net.*;

public class SimpleWebClient2 implements Runnable {

    public void run(){
        Socket socket = null;
        PrintWriter writer = null;
        BufferedReader reader = null;

        try {
            // Establish a connection to the server
            socket = new Socket("localhost", 5000);
            System.out.println("Connected to server");

            // Create an output stream to send the request
            OutputStream out = socket.getOutputStream();

            // Create a PrintWriter to write the request
            writer = new PrintWriter(new OutputStreamWriter(out));

            // Prepare the POST request with form data
            String postData = "account=123&value=1&toAccount=321&toValue=1";
            //create a random number between 1000 and 60000
            int waitfor = (int)(Math.random() * 1000 + 200);
            Thread.sleep(waitfor);
            // Send the POST request
            writer.println("POST /submit HTTP/1.1");
            writer.println("Host: localhost:8080");
            writer.println("Content-Type: application/x-www-form-urlencoded");
            writer.println("Content-Length: " + postData.length());
            writer.println();
            writer.println(postData);
            writer.flush();

            // Create an input stream to read the response
            InputStream in = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(in));

            // Read and print the response
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch(InterruptedException | IOException e){
            e.printStackTrace();
        }finally {
            // Close the streams and socket
            try {
                reader.close();
                writer.close();
                socket.close();
            } catch (IOException e) {

                e.printStackTrace();

            }

        }
    }
    // Main method to create threads
    public static void main(String[] args) throws InterruptedException {
        //create 1000 clients
        for(int i = 0; i < 1000; i++){
            Thread thread = new Thread(new SimpleWebClient2());
            Thread.sleep(1);
            thread.start();
        }
    }
}
