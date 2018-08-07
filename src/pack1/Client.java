package pack1;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;

public class Client extends JFrame {
    private JButton sendButton;
    private JPanel contentPane;
    private JButton shareFileButton;
    private JLabel infoLabel;
    private JTextField inputField;
    private JTextArea textArea;
    private JTextArea userText;
    private JButton showSharedFilesButton;
    private JButton e1;
    private JButton e4;
    private JButton e3;
    private JButton e6;
    private JButton e2;
    private JButton e5;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Socket socket;
    private String serverIP;
    private int serverPort;
    private String user;
    private JFileChooser fileChooser;
    private String[] users;

    {

        $$$setupUI$$$();
    }

    public Client(String host, int port, final String user) {
        super("MessenJ IM - Client");
        setContentPane(contentPane);
        setSize(640, 480);
        setLocationByPlatform(true);
        getRootPane().setDefaultButton(sendButton);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        fileChooser = new JFileChooser();

        if (!user.equals("")) {
            this.user = user;
        } else this.user = "Client";
        serverIP = host;
        serverPort = port;
        allowTyping(false);
        inputField.addActionListener(e -> {
            String text = e.getActionCommand();
            if (!text.equals("")) {
                send(new Message(Message.TYPE_TEXT, text, user));
                inputField.setText("");
            }
        });
        sendButton.addActionListener(e -> {
            String text = inputField.getText();
            if (!text.equals("")) {
                send(new Message(Message.TYPE_TEXT, text, user));
                inputField.setText("");
            }
        });
        shareFileButton.addActionListener(e -> {
            if (fileChooser.showOpenDialog(contentPane) == JFileChooser.APPROVE_OPTION) {
                Message message;
                try {
                    File file = fileChooser.getSelectedFile();
                    message = new Message(Message.TYPE_FILE, Files.readAllBytes(file.toPath()), file.getName(), user);
                    showMessage(new Message("Sending file " + file.getName()));
                    send(message);
                } catch (IOException e1) {
                    showMessage(new Message("Error sending file"));
                    e1.printStackTrace();
                }
            }
        });
        showSharedFilesButton.addActionListener(e -> {
            try {
                new File(System.getProperty("user.home")
                        + File.separator
                        + "chat_app")
                        .mkdirs();
                Desktop.getDesktop().open(new File(System.getProperty("user.home")
                        + File.separator
                        + "char_app"));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        //setupEmoji();
        setVisible(true);
        inputField.requestFocus();
        setup();
    }

    private void setupEmoji() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Thread.currentThread().getContextClassLoader().getResource("OpenSansEmoji.ttf").openStream()).deriveFont(16.0f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(font);
            inputField.setFont(font);
            textArea.setFont(font);
            e1.setText("\u263A");
            e1.setFont(font);
            e1.addActionListener(e -> {
                if (inputField.isEditable()) inputField.setText(inputField.getText() + e1.getText());
            });
            e2.setText("\uD83D\uDE02");
            e2.setFont(font);
            e2.addActionListener(e -> {
                if (inputField.isEditable()) inputField.setText(inputField.getText() + e2.getText());
            });
            e3.setText("\uD83D\uDE22");
            e3.setFont(font);
            e3.addActionListener(e -> {
                if (inputField.isEditable()) inputField.setText(inputField.getText() + e3.getText());
            });
            e4.setText("\uD83D\uDC4F");
            e4.setFont(font);
            e4.addActionListener(e -> {
                if (inputField.isEditable()) inputField.setText(inputField.getText() + e4.getText());
            });
            e5.setText("\uD83D\uDC4D");
            e5.setFont(font);
            e5.addActionListener(e -> {
                if (inputField.isEditable()) inputField.setText(inputField.getText() + e5.getText());
            });
            e6.setText("\uD83D\uDC4E");
            e6.setFont(font);
            e6.addActionListener(e -> {
                if (inputField.isEditable()) inputField.setText(inputField.getText() + e6.getText());
            });
        } catch (NullPointerException | FontFormatException | IOException e) {
            e.printStackTrace();
            showMessage(new Message("Failed to load thedorkknightrises.messenj.fonts, emoji may not display properly"));
        }
    }

    private void setup() {
        try {
            connect();
            setupStreams();
            whileConnected();
        } catch (EOFException e) {
            showMessage(new Message("Client terminated connection"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    private void allowTyping(final boolean allowed) {
        SwingUtilities.invokeLater(() -> {
            inputField.setEditable(allowed);
            sendButton.setEnabled(allowed);
            shareFileButton.setEnabled(allowed);
        });
    }

    private void send(Message message) {
        try {
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            showMessage(new Message("Couldn\'t send your message"));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void showMessage(final Message message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.getType()) {
                case Message.TYPE_CONNECT:
                    textArea.append(message.getText() + "\n");
                    users[message.getNumber() + 1] = message.getSender();
                    updateUserList();
                    break;
                case Message.TYPE_DISCONNECT:
                    users[message.getNumber() + 1] = null;
                    updateUserList();
                case Message.TYPE_ANNOUNCE:
                    textArea.append(message.getText() + "\n");
                    break;
                case Message.TYPE_TEXT:
                    textArea.append(message.getSender() + ": " + message.getText() + "\n");
                    break;
                case Message.TYPE_FILE:
                    if (!message.getSender().equals(user)) {
                        new File(System.getProperty("user.home")
                                + File.separator
                                + "MessenJ")
                                .mkdirs();
                        File newFile = new File(System.getProperty("user.home")
                                + File.separator
                                + "MessenJ"
                                + File.separator
                                + message.getText());
                        showMessage(new Message(message.getSender() + " is sending file: " + message.getText()));
                        new Thread(() -> {
                            try {
                                FileOutputStream writer = new FileOutputStream(newFile);
                                writer.write(message.getData());
                                writer.close();
                                showMessage(new Message("File transfer from " + message.getSender() + " complete"));
                                showMessage(new Message("Saved to: " + newFile.toPath()));
                            } catch (IOException e) {
                                showMessage(new Message("Error receiving file"));
                            }
                        }).start();
                    } else textArea.append("Successfully uploaded " + message.getText() + "\n");
                    break;
            }
        });
    }

    private void updateUserList() {
        userText.setText("");
        for (int i = 0; i < users.length; i++) {
            if (users[i] != null) userText.append(users[i] + "\n");
        }
        userText.updateUI();
    }

    private void connect() throws IOException {
        try {
            showMessage(new Message("Attempting connection to server..."));
            socket = new Socket(InetAddress.getByName(serverIP), serverPort);
            showMessage(new Message("Connecting to " + socket.getInetAddress().getHostName() + " (waiting in queue)"));
        } catch (ConnectException e) {
            showMessage(new Message("Could not connect to server at that address"));
        }
    }

    private void setupStreams() {
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            outputStream.writeObject(user);
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());
            showMessage(new Message("Connection established"));
            infoLabel.setText("Connected as " + user + " to " + socket.getInetAddress().getHostName() + ":" + serverPort);
            users = (String[]) inputStream.readObject();
            updateUserList();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            showMessage(new Message("Error fetching list of users"));
            e.printStackTrace();
        }
    }

    private void whileConnected() throws IOException {
        Message message;
        allowTyping(true);
        do {
            try {
                message = (Message) inputStream.readObject();
                showMessage(message);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                showMessage(new Message("Something went wrong, cannot display message"));
            } catch (EOFException e) {
            } catch (SocketException e) {
                e.printStackTrace();
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        } while (true);
    }

    private void close() {
        showMessage(new Message("Closing all connections..."));
        try {
            inputStream.close();
            outputStream.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            showMessage(new Message("All connections closed."));
            allowTyping(false);
            infoLabel.setText("Not connected");
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(5, 3, new Insets(8, 8, 8, 8), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        contentPane.add(scrollPane1, new GridConstraints(1, 0, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font(textArea.getFont().getName(), textArea.getFont().getStyle(), textArea.getFont().getSize()));
        textArea.setText("");
        scrollPane1.setViewportView(textArea);
        inputField = new JTextField();
        contentPane.add(inputField, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        infoLabel = new JLabel();
        infoLabel.setText("Not connected");
        contentPane.add(infoLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        contentPane.add(scrollPane2, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        userText = new JTextArea();
        userText.setEditable(false);
        userText.setFont(new Font(userText.getFont().getName(), userText.getFont().getStyle(), userText.getFont().getSize()));
        userText.setText("");
        scrollPane2.setViewportView(userText);
        sendButton = new JButton();
        sendButton.setText("Send");
        contentPane.add(sendButton, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        shareFileButton = new JButton();
        shareFileButton.setText("Share file");
        contentPane.add(shareFileButton, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Connected users:");
        contentPane.add(label1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showSharedFilesButton = new JButton();
        showSharedFilesButton.setText("Show shared files");
        contentPane.add(showSharedFilesButton, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        e1 = new JButton();
        e1.setText("e1");
        panel1.add(e1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        e4 = new JButton();
        e4.setText("e4");
        panel1.add(e4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        e3 = new JButton();
        e3.setText("e3");
        panel1.add(e3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        e6 = new JButton();
        e6.setText("e6");
        panel1.add(e6, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        e2 = new JButton();
        e2.setText("e2");
        panel1.add(e2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        e5 = new JButton();
        e5.setText("e5");
        panel1.add(e5, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
