package com.enawga.chat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient extends Application {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;
    private static final String LEGACY_NAME_PROMPT = "Enter your name:";

    private final TextArea chatArea = new TextArea();
    private final TextField hostField = new TextField(DEFAULT_HOST);
    private final TextField portField = new TextField(String.valueOf(DEFAULT_PORT));
    private final TextField nameField = new TextField();
    private final TextField messageField = new TextField();
    private final Button connectButton = new Button("Connect");
    private final Button sendButton = new Button("Send");

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listenerThread;
    private volatile boolean connected;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPromptText("Chat messages will appear here...");

        hostField.setPrefColumnCount(10);
        portField.setPrefColumnCount(6);
        nameField.setPromptText("Your name");
        messageField.setPromptText("Type a message and press Enter");
        messageField.setDisable(true);
        sendButton.setDisable(true);

        connectButton.setOnAction(event -> connect());
        sendButton.setOnAction(event -> sendMessage());
        messageField.setOnAction(event -> sendMessage());

        HBox connectionBar = new HBox(8,
                new Label("Host"), hostField,
                new Label("Port"), portField,
                new Label("Name"), nameField,
                connectButton);
        connectionBar.setAlignment(Pos.CENTER_LEFT);

        HBox messageBar = new HBox(8, messageField, sendButton);
        messageBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(messageField, javafx.scene.layout.Priority.ALWAYS);

        VBox topSection = new VBox(12, connectionBar, messageBar);
        topSection.setPadding(new Insets(12));

        ScrollPane scrollPane = new ScrollPane(chatArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        BorderPane root = new BorderPane();
        root.setTop(topSection);
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 760, 520);
        stage.setTitle("Enawga Chat");
        stage.setScene(scene);
        stage.show();
        nameField.requestFocus();

        stage.setOnCloseRequest(event -> disconnect());
        appendMessage("Connect to the chat server to begin.");
    }

    private void connect() {
        if (connected) {
            appendMessage("Already connected.");
            return;
        }

        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            host = DEFAULT_HOST;
        }

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            appendMessage("Port must be a valid number.");
            return;
        }

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            appendMessage("Enter a name before connecting.");
            return;
        }

        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            connected = true;
            connectButton.setDisable(true);
            hostField.setDisable(true);
            portField.setDisable(true);
            nameField.setDisable(true);
            messageField.setDisable(false);
            sendButton.setDisable(false);

            out.println(name);
            appendMessage("Connected to " + host + ":" + port + " as " + name + ".");

            listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();
        } catch (IOException ex) {
            appendMessage("Connection error: " + ex.getMessage());
            disconnect();
        }
    }

    private void sendMessage() {
        if (!connected || out == null) {
            appendMessage("Connect to the server first.");
            return;
        }

        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        out.println(message);
        appendMessage(nameField.getText().trim() + ": " + message);
        messageField.clear();

        if ("/quit".equalsIgnoreCase(message)) {
            disconnect();
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (LEGACY_NAME_PROMPT.equals(message.trim())) {
                    continue;
                }

                String incoming = message;
                Platform.runLater(() -> appendMessage(incoming));
            }
        } catch (IOException ex) {
            Platform.runLater(() -> appendMessage("Disconnected: " + ex.getMessage()));
        } finally {
            Platform.runLater(this::disconnect);
        }
    }

    private void disconnect() {
        connected = false;
        connectButton.setDisable(false);
        hostField.setDisable(false);
        portField.setDisable(false);
        nameField.setDisable(false);
        messageField.setDisable(true);
        sendButton.setDisable(true);

        try {
            if (out != null) {
                out.println("/quit");
            }
        } catch (Exception ignored) {
        }

        closeQuietly();
    }

    private void closeQuietly() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {
        }

        if (out != null) {
            out.close();
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }

        in = null;
        out = null;
        socket = null;
    }

    private void appendMessage(String message) {
        chatArea.appendText(message + System.lineSeparator());
    }
}