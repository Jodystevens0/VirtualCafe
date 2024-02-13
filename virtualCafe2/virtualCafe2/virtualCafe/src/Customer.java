import java.io.*;
import java.net.*;

// Define the Customer class
public class Customer {
    // Define constant values for server IP address and port number
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 9999;

    // Main method
    public static void main(String[] args) {
        // Use try-with-resources to automatically close resources like sockets and readers
        try (
                // Create a socket to connect to the server with the specified IP address and port
                Socket socket = new Socket(SERVER_IP, PORT);

                // Create a PrintWriter to send data to the server
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Create a BufferedReader to receive data from the server
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Create a BufferedReader to read user input from the console
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))
        ) {
            // Display a message indicating that the client is connected to the virtual cafe
            System.out.println("Connected to the Virtual Cafe, enter your name or type 'exit' to leave.");

            // String to store user input
            String userInputStr;

            // Read user input from the console in a loop
            while ((userInputStr = userInput.readLine()) != null) {
                // Send the user input to the server
                out.println(userInputStr);

                // Receive and print the server's response
                String serverResponse = in.readLine();
                System.out.println("Server: " + serverResponse);

                // Check if the user wants to exit the program
                if (userInputStr.equalsIgnoreCase("exit")) {
                    break;
                }
            }
        } catch (IOException e) {
            // Print any IOException that occurs
            e.printStackTrace();
        }
    }
}
