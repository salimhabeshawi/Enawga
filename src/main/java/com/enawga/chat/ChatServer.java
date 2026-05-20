package com.enawga.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {
    private static final int DEFAULT_PORT = 5000;
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new ChatServer().start(port);
    }

    private void start(int port) {
        System.out.println("Chat server starting on port " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException ex) {
            System.err.println("Server error: " + ex.getMessage());
        }
    }

    private void broadcast(String message, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.send(message);
                }
            }
        }
    }

    private void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private String clientName = "unknown";

        private ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);
                clientName = in.readLine();

                if (clientName == null || clientName.isBlank()) {
                    clientName = "Guest-" + socket.getPort();
                }

                broadcast(clientName + " joined the chat.", this);
                System.out.println(clientName + " connected from " + socket.getRemoteSocketAddress());

                String line;
                while ((line = in.readLine()) != null) {
                    if ("/quit".equalsIgnoreCase(line.trim())) {
                        break;
                    }
                    String formatted = clientName + ": " + line;
                    System.out.println(formatted);
                    broadcast(formatted, this);
                }
            } catch (IOException ex) {
                System.err.println("Client error for " + clientName + ": " + ex.getMessage());
            } finally {
                broadcast(clientName + " left the chat.", this);
                removeClient(this);
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        private void send(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }
}
