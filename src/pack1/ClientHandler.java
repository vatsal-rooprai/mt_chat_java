package pack1;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {
    ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private Server server;
    private Socket socket;
    private int number;

    public ClientHandler(Server server, Socket socket, int number) {
        this.server = server;
        this.socket = socket;
        this.number = number;
    }

    @Override
    public void run() {
        setup();
        whileConnected();
        close();
    }

    private void setup() {
        try {
            inputStream = new ObjectInputStream(socket.getInputStream());
            String username = (String) inputStream.readObject();
            server.users[number + 1] = username;
            server.send(new Message(Message.TYPE_CONNECT, number, username + " joined the conversation", username));
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            outputStream.writeObject(server.users);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            server.showMessage(new Message("Error fetching username"));
            e.printStackTrace();
        }
    }

    private void whileConnected() {
        Object object;
        Message message;
        server.allowTyping(true);
        do {
            try {
                object = inputStream.readObject();
                if (object instanceof Message) {
                    message = (Message) object;
                    server.send(message);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                server.showMessage(new Message("Something went wrong, cannot display message"));
            } catch (EOFException e) {
                close();
            } catch (SocketException e) {
                e.printStackTrace();
                close();
                break;
            } catch (IOException e) {
                e.printStackTrace();
                close();
                break;
            }
        } while (true);
    }

    private void close() {
        if (server.users[number + 1] != null)
        server.showMessage(new Message("Closing connection with " + server.users[number + 1]));
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.disconnected(number);
    }
}
