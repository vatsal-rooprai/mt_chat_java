package pack1;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;

public class Server extends JFrame {
    JLabel infoLabel;
    String[] users;
    private JTextField inputField;
    private ServerSocket serverSocket;
    private int connections;
    private int port;
    private String user;
    private int number;
    private JButton sendButton;
    private JPanel contentPane;
    private JButton shareFileButton;
    private JTextArea userText;
    private JTextArea textArea;
    private JButton showSharedFilesButton;
    private JPanel emojiPanel;
    private JButton e1;
    private JButton e2;
    private JButton e3;
    private JButton e4;
    private JButton e5;
    private JButton e6;
    private JFileChooser fileChooser;
    private ClientHandler[] clientHandlers;

    {
        $$$setupUI$$$();
    }

    public Server(int port, final String user, int number) {
        super("MessenJ IM - Server");
        setContentPane(contentPane);
        setSize(640, 480);
        setLocationByPlatform(true);
        getRootPane().setDefaultButton(sendButton);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        fileChooser = new JFileChooser();

        connections = 0;
        this.port = port;
        this.user = user;
        this.number = number;
        users = new String[number + 1];
        users[0] = user;
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
                        + "MessenJ")
                        .mkdirs();
                Desktop.getDesktop().open(new File(System.getProperty("user.home")
                        + File.separator
                        + "MessenJ"));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        //setupEmoji();
        setVisible(true);
        inputField.requestFocus();
        clientHandlers = new ClientHandler[number];
        for (int i = 0; i < number; i++) clientHandlers[i] = null;
        setupServer();
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
            return;
        }
    }

    private void setupServer() {
        try {
            serverSocket = new ServerSocket(port);
            infoLabel.setText("Username: " + user + " | External IP: " + getExtIp() + " | Port: " + port);
            showMessage(new Message("Waiting to connect..."));
            while (connections < number) {
                waitForConnection(connections);
            }
        } catch (BindException e) {
            showMessage(new Message("This port is already in use. Try hosting on a different port"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void allowTyping(final boolean allowed) {
        SwingUtilities.invokeLater(() -> {
            inputField.setEditable(allowed);
            sendButton.setEnabled(allowed);
            shareFileButton.setEnabled(allowed);
        });
    }

    void send(Message message) {
        for (int i = 0; i < number; i++) {
            try {
                clientHandlers[i].outputStream.writeObject(message);
                clientHandlers[i].outputStream.flush();
            } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            } catch (IOException e) {
                e.printStackTrace();
                showMessage(new Message("Couldn\'t send your message to user #" + (i + 1)));
            }
        }
        showMessage(message);
    }

    void showMessage(final Message message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.getType()) {
                case Message.TYPE_CONNECT:
                    textArea.append(message.getText() + "\n");
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
                                + "chat_app")
                                .mkdirs();
                        File newFile = new File(System.getProperty("user.home")
                                + File.separator
                                + "chat_app"
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
        for (String user1 : users) if (user1 != null) userText.append(user1 + "\n");
        userText.updateUI();
    }

    private void waitForConnection(int n) {
        if (clientHandlers[n] == null) {
            try {
                clientHandlers[n] = new ClientHandler(this, serverSocket.accept(), n);
                showMessage(new Message("Incoming connection..."));
                new Thread(clientHandlers[n]).start();
                connections++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void disconnected(int number) {
        clientHandlers[number] = null;
        connections--;
        if (users[number + 1] != null)
            send(new Message(Message.TYPE_DISCONNECT, number, users[number + 1] + " disconnected", users[number + 1]));
        users[number + 1] = null;
        waitForConnection(number);
    }

    private String getExtIp() {
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(
                        whatismyip.openStream()));
                return in.readLine();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            return "-";
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
        inputField = new JTextField();
        contentPane.add(inputField, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        infoLabel = new JLabel();
        infoLabel.setText("Not connected");
        contentPane.add(infoLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        contentPane.add(scrollPane1, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        userText = new JTextArea();
        userText.setEditable(false);
        userText.setFont(new Font(userText.getFont().getName(), userText.getFont().getStyle(), userText.getFont().getSize()));
        scrollPane1.setViewportView(userText);
        sendButton = new JButton();
        sendButton.setText("Send");
        contentPane.add(sendButton, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        shareFileButton = new JButton();
        shareFileButton.setText("Share file");
        contentPane.add(shareFileButton, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Connected users:");
        contentPane.add(label1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        contentPane.add(scrollPane2, new GridConstraints(1, 0, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textArea = new JTextArea();
        textArea.setEditable(false);
        scrollPane2.setViewportView(textArea);
        showSharedFilesButton = new JButton();
        showSharedFilesButton.setText("Show shared files");
        contentPane.add(showSharedFilesButton, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        emojiPanel = new JPanel();
        emojiPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(emojiPanel, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        e1 = new JButton();
        e1.setText("e1");
        emojiPanel.add(e1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        e3 = new JButton();
        e3.setText("e3");
        emojiPanel.add(e3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        e2 = new JButton();
        e2.setText("e2");
        emojiPanel.add(e2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        e4 = new JButton();
        e4.setText("e4");
        emojiPanel.add(e4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        e5 = new JButton();
        e5.setText("e5");
        emojiPanel.add(e5, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        e6 = new JButton();
        e6.setText("e6");
        emojiPanel.add(e6, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
