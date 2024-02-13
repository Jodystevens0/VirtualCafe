import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Barista {
    private static final int PORT = 9999;  // Defines the port number for the server
    private static final int MAX_CLIENTS = 10;  // Sets the maximum number of clients allowed
    private static ExecutorService clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENTS);  // Creates a thread pool for clients
    private static BlockingQueue<Socket> waitingArea = new LinkedBlockingQueue<>();  // Initializes a queue for clients waiting to be served
    private static BlockingQueue<String> brewingArea = new LinkedBlockingQueue<>(2); // Limit brewing to 2  // Sets a limit for orders being brewed at once
    private static BlockingQueue<String> trayArea = new LinkedBlockingQueue<>(10);  // Sets a limit for orders placed in the tray

    // Map to store client names and their respective status
    private static ConcurrentHashMap<String, String> clientStatus = new ConcurrentHashMap<>();  // Stores client names and their status

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Barista started. Waiting for clients...");  // Notifies the start of the Barista server

            while (true) {
                if (clientThreadPool instanceof ThreadPoolExecutor) {  // Checks if the client thread pool is of type ThreadPoolExecutor
                    ThreadPoolExecutor pool = (ThreadPoolExecutor) clientThreadPool;

                    if (pool.getQueue().remainingCapacity() <= 0) {  // Checks if the client pool queue has reached its capacity
                        Socket clientSocket = serverSocket.accept();  // Accepts a client connection
                        System.out.println("Waiting area reached. Placing client in the waiting area.");  // Informs that the waiting area is full, placing client in the queue
                        waitingArea.put(clientSocket);  // Puts the client in the waiting area queue
                    } else {
                        Socket clientSocket = serverSocket.accept();  // Accepts a new client connection
                        System.out.println("New client connected.");  // Notifies about a new client connection

                        ClientHandler clientHandler = new ClientHandler(clientSocket);  // Creates a handler for the new client
                        clientThreadPool.execute(clientHandler);  // Executes the client handler in the thread pool
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();  // Handles and prints any IO or InterruptedException
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;  // Socket for the client
        private PrintWriter out;  // Output stream for client
        private BufferedReader in;  // Input stream for client

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;  // Initializes the client socket
        }

        public void run() {
            try {
                boolean orderDelivered = false;  // Flags whether the order has been delivered or not
                out = new PrintWriter(clientSocket.getOutputStream(), true);  // Initializes the output stream for the client
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));  // Initializes the input stream for the client

                String clientName = in.readLine();  // Reads the client's name
                out.println("Hello, " + clientName + "! You are now in the cafe.");  // Greets the client and informs them of their status in the cafe
                clientStatus.put(clientName, "idle");  // Sets the client's initial status to idle

                String inputLine;
                String order = null;  // Initializes the order

                while ((inputLine = in.readLine()) != null) {  // Reads client inputs until null is received

                    if (inputLine.equalsIgnoreCase("hello")) {  // Checks if the input is 'hello'
                        out.println("Welcome");  // Greets the client
                    } else if (inputLine.equalsIgnoreCase("I want to place an order")) {  // Checks if the client wants to place an order
                        out.println("Please enter your order details in 1 line:");  // Asks the client to enter their order details
                        order = in.readLine();  // Reads the order details from the client

                        // Handles different order types based on conditions
                        brewingArea.put(clientName);  // Puts the client in the brewing area queue

                        // Differentiates order preparation times based on the order contents
                        // Signals the time needed for preparing the order
                        // Clears the brewing area after the order is prepared and sets the order as delivered
                        // Conditions separated by the content of the order for tea, coffee, or both
                        // Time given to prepare each type of order

                        if(order.contains("tea") && !order.contains("coffee")){
                            out.println("Please wait for 30s to prepare your order: " + order+"\n order is in tray area : "+ order);
                            TimeUnit.SECONDS.sleep(30);
                            brewingArea.clear();
                            orderDelivered = true;

                               out.println("Order delivered for " + clientName+": ( "+order+" )");

                            // Informs the client about the order delivery
                        }else if(order.contains("coffee") && !order.contains("tea")){
                            out.println("Please wait for 45s to prepare your order: " + order+"\n order is in tray area : "+ order);
                            TimeUnit.SECONDS.sleep(45);
                            brewingArea.clear();
                            orderDelivered = true;
                            out.println("Order delivered for " + clientName+": ( "+order+" )");  // Informs the client about the order delivery

                        }else if(order.contains("coffee") && order.contains("tea")){
                            out.println("Please wait for 75s to prepare your order: " + order+"\n order is in tray area : "+ order);
                            TimeUnit.SECONDS.sleep(75);
                            // The brewing area is then cleared and the order is marked as delivered
                            brewingArea.clear();
                            orderDelivered = true;

                            out.println("Order delivered for " + clientName+": ( "+order+" )");  // Informs the client about the order delivery

                        }

                    } else if (inputLine.equalsIgnoreCase("order status")) {  // Checks if the client wants to know their order status

                        // Provides the order status to the client based on whether the order is delivered or not
                        if (orderDelivered) {
                           out.println("Order delivered for " + clientName+": ( "+order+" )");
                        } else {
                            out.println("No order yet for " + clientName);
                        }

                        trayArea.put(clientName);  // Places the client in the tray area queue
                    } else if (inputLine.equalsIgnoreCase("exit")) {  // Checks if the client wants to exit
                        out.println("Bye! Exiting the cafe.");  // Says goodbye to the client and exits the cafe
                        break;  // Breaks the loop
                    } else {
                        out.println("Unknown command");  // Notifies the client about an unknown command
                    }
                }

                if (clientThreadPool instanceof ThreadPoolExecutor) {  // Checks if the client thread pool is of type ThreadPoolExecutor
                    ThreadPoolExecutor pool = (ThreadPoolExecutor) clientThreadPool;

                    if (pool.getQueue().remainingCapacity() > 0) {  // Checks if the client pool queue has remaining capacity
                        Socket waitingClient = waitingArea.poll();  // Retrieves a waiting client from the waiting area queue
                        if (waitingClient != null) {  // Checks if there is a waiting client
                            ClientHandler clientHandler = new ClientHandler(waitingClient);  // Creates a handler for the waiting client
                            clientThreadPool.execute(clientHandler);  // Executes the handler for the waiting client in the thread pool
                            out.println("You are now in the cafe from the waiting area.");  // Informs the waiting client about their status change
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();  // Handles and prints any IO exceptions
            } catch (InterruptedException e) {
                throw new RuntimeException(e);  // Throws a runtime exception for InterruptedException
            }
        }
    }
}

