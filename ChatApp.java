import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatApp {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("server")) {
            startServer();
        } else {
            startClient();
        }
    }

    // --- SERVER LOGIC ---
    private static void startServer() {
        Set<PrintWriter> clientWriters = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] CHAT SERVER STARTED ON PORT " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                pool.execute(new Handler(socket, clientWriters));
            }
        } catch (IOException e) {
            System.err.println("[SERVER ERROR] " + e.getMessage());
        }
    }

    private static class Handler implements Runnable {
        private Socket socket;
        private Set<PrintWriter> writers;
        private String name;

        public Handler(Socket socket, Set<PrintWriter> writers) {
            this.socket = socket;
            this.writers = writers;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                
                out.println("SUBMIT_NAME");
                name = in.readLine();
                System.out.println("[SERVER] " + name + " HAS CONNECTED.");
                writers.add(out);
                
                for (PrintWriter writer : writers) writer.println("[SYSTEM] " + name + " JOINED.");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("/quit")) break;
                    for (PrintWriter writer : writers) writer.println(name + ": " + message);
                }
            } catch (IOException e) {
                System.out.println("[SERVER] CONNECTION LOST WITH " + name);
            } finally {
                System.out.println("[SERVER] " + name + " DISCONNECTED.");
            }
        }
    }

    // --- CLIENT LOGIC ---
    private static void startClient() {
        try (Socket socket = new Socket("localhost", PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            // THREAD TO LISTEN FOR SERVER MESSAGES
            Thread listener = new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = in.readLine()) != null) {
                        if (serverMsg.equals("SUBMIT_NAME")) {
                            System.out.print("ENTER YOUR USERNAME: ");
                        } else {
                            System.out.println("\n" + serverMsg);
                        }
                    }
                } catch (IOException e) { System.out.println("DISCONNECTED FROM SERVER."); }
            });
            listener.start();

            // MAIN THREAD HANDLES USER INPUT
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                out.println(input);
                if (input.equalsIgnoreCase("/quit")) break;
            }
        } catch (IOException e) {
            System.err.println("[CLIENT ERROR] COULD NOT CONNECT TO SERVER.");
        }
    }
}
