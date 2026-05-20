# Enawga Chat

Simple Java chat app with a socket server and a JavaFX client.

## Run

Compile the project:

```bash
mvn clean compile
```

Start the server:

```bash
java -cp target/classes com.enawga.chat.ChatServer
```

Start the client in two separate terminals:

```bash
mvn javafx:run
```

## Test with two clients

1. Start the server.
2. Open two client windows with `mvn javafx:run`.
3. In each client, enter a name and click Connect.
4. Send a message from one client and see it appear in the other.
