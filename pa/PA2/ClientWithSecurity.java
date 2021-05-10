import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Scanner;
import javax.crypto.SecretKey;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.FontUIResource;

public class ClientWithSecurity {
    private static final String PRIVATE_KEY_FILENAME = "credentials/client/private_key.der";
    private static final String PUBLIC_KEY_FILENAME = "credentials/client/public_key.der";
    public static PublicKey publicServerKey;
    public static SecretKey sessionKey; // Only for CP2
    private static String username;
    private static String password;
    private static String mode = "CP2"; // CP1 or CP2 (default is CP2)
    // This is to assist us in the future in the case whereby client and server are on separate
    // machines/computers. Need to send over encrypted abstract representation of the files'
    // metadata.
    private static boolean serverDirectoryIsListed = false;

    public static void main(String[] args) {
        // Prevent execution if user never specifies mode of operation
        if (args.length <= 0) {
            System.out.println("Please specify the mode of operation for the program! Quitting...");
            System.exit(1);
        }

        // Initial setup
        Utils.makeFolder("download");

        // Default server and port number
        String serverAddress = "localhost";
        int port = 4321; // This will serve as base port number if multi-threaded

        // Generate client-side nonce to verify that client is talking to a live server
        final int nonce = new SecureRandom().nextInt();

        // TODO: Generate client's sequence number to prevent partial replay/playback
        // attacks
        int sequence = new SecureRandom().nextInt();
        int packetCount = 0;

        // Start timer
        long timeStarted = System.nanoTime();

        // Switch to command-line arguments mode
        if (args[0].equalsIgnoreCase("CLI")) {
            // Argument parsing section
            if (args.length > 1)
                serverAddress = args[1];

            if (args.length > 2)
                port = Integer.parseInt(args[2]);

            if (args.length > 3)
                mode = args[3];

            if (!mode.equalsIgnoreCase("CP1") && !mode.equalsIgnoreCase("CP2")) {
                System.out.println("Invalid mode specified!");
                System.exit(1);
            }

            if (args.length > 4)
                username = args[4];

            if (args.length > 5)
                password = args[5];

            System.out.println("Establishing connection to server...");

            // Attempt to connect to server
            try (Socket clientSocket = new Socket(serverAddress, port)) {
                // Get the input and output streams
                DataOutputStream toServer = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream fromServer = new DataInputStream(clientSocket.getInputStream());

                // Transmit mode
                toServer.writeInt(PacketTypes.CHANGE_MODE_PACKET.getValue());
                if (mode.equalsIgnoreCase("CP1"))
                    toServer.writeInt(PacketTypes.CP1_MODE_PACKET.getValue());
                else
                    toServer.writeInt(PacketTypes.CP2_MODE_PACKET.getValue());

                Scanner sc = null;

                // Prompt for login first, only if user never specifies username and password as
                // arguments
                if ((username == null) || (password == null)) {
                    sc = new Scanner(System.in);
                    System.out.print("Username: ");
                    username = sc.nextLine();
                    System.out.print("Password: ");
                    password = sc.nextLine();
                    sc.close();
                }

                // Authenticate server and assign server's public key to variable
                int decryptedNonce = 0;
                System.out.println("Authenticating server...");
                try {
                    // Request authentication
                    toServer.writeInt(PacketTypes.VERIFY_SERVER_PACKET.getValue());
                    toServer.flush();
                    decryptedNonce = Utils.authenticate(nonce, toServer, fromServer,
                            "challenge_the_server", mode);
                } catch (Exception e) {
                    System.out.println("Authentication failed!");
                    clientSocket.close();
                    System.exit(1);
                }

                if (decryptedNonce != nonce) {
                    System.out.println("Failed to authenticate server!");
                    clientSocket.close();
                    System.exit(1);
                }

                System.out.println("Server is authenticated!");

                PrivateKey privateClientKey = PrivateKeyReader.get(PRIVATE_KEY_FILENAME);
                RSAKeyHelper rsaKeyHelper = new RSAKeyHelper(publicServerKey, privateClientKey);

                int packetType = fromServer.readInt();
                if (packetType == PacketTypes.VERIFY_CLIENT_PACKET.getValue()) {
                    Utils.acceptChallenge(toServer, fromServer, privateClientKey,
                            PUBLIC_KEY_FILENAME);
                }

                // Verify user (separation of concerns from liveness check)
                try {
                    toServer.writeInt(PacketTypes.AUTH_LOGIN_USER_PACKET.getValue());
                    // Send username first for server to identify user
                    byte[] encryptedUsername = rsaKeyHelper.encryptWithPublic(username.getBytes());
                    toServer.writeInt(encryptedUsername.length);
                    toServer.write(encryptedUsername);
                    toServer.flush();
                    // Then send hash of concatenation of username and password
                    String hash = Utils.generateHash(username, password);
                    byte[] encryptedHash = rsaKeyHelper.encryptWithPublic(hash.getBytes());
                    toServer.writeInt(encryptedHash.length);
                    toServer.write(encryptedHash);
                    toServer.flush();
                } catch (RuntimeException e) {
                    System.out.println("Some unexpected behavior has occurred! Quitting...");
                    System.exit(1);
                }

                if (fromServer.readInt() != PacketTypes.OK_PACKET.getValue()) {
                    System.out.println("Wrong username or password!");
                    clientSocket.close();
                    System.exit(1);
                }

                System.out.println("Welcome, " + username + "!");

                // Still share session key with server, even if current mode is not CP2.
                // This allows the client and server to switch back and forth between CP1 and
                // CP2 independently without much fuss of attempting to transfer the session key
                // midway.
                Utils.doClientSessionKey(toServer, publicServerKey);
                AESCipherHelper aesCipherHelper = new AESCipherHelper(sessionKey);

                String command = args[6];
                String[] files = null;

                // Grab all the filenames from the rest of the command-line arguments
                if (args.length > 7) {
                    files = Arrays.copyOfRange(args, 7, args.length);
                }

                if (command.equals("UPLD")) {
                    if (files != null) {
                        try {
                            for (String file : files) {
                                String filename = "data/" + file;
                                toServer.writeInt(PacketTypes.UPLOAD_FILE_PACKET.getValue());
                                int packets = ((mode.equalsIgnoreCase("CP1"))
                                        ? Utils.sendEncryptedFile(toServer, filename,
                                                publicServerKey, "CP1")
                                        : Utils.sendEncryptedFile(toServer, filename, sessionKey,
                                                "CP2"));
                                packetCount += packets;
                                int numBytes = fromServer.readInt();
                                byte[] buffer = new byte[numBytes];
                                fromServer.readFully(buffer, 0, numBytes);
                                System.out.println(new String(buffer, 0, numBytes));
                            }
                        } catch (Exception e) {
                            System.out.println("Invalid file specified!");
                        }
                    } else {
                        System.out.println("Please specify the file(s) to be uploaded!");
                    }
                    toServer.writeInt(PacketTypes.STOP_PACKET.getValue());
                } else if (command.equals("DWNLD")) {
                    if (files != null) {
                        for (String file : files) {
                            toServer.writeInt(PacketTypes.DOWNLOAD_FILE_PACKET.getValue());
                            byte[] encryptedFilename = ((mode.equalsIgnoreCase("CP1"))
                                    ? rsaKeyHelper.encryptWithPublic(file.getBytes())
                                    : aesCipherHelper.encryptWithKey(file.getBytes()));
                            toServer.writeInt(encryptedFilename.length);
                            toServer.write(encryptedFilename);
                            toServer.flush();
                            packetCount += 3;
                            int fileExists = fromServer.readInt();
                            if (fileExists == PacketTypes.UPLOAD_FILE_PACKET.getValue()) {
                                if (mode.equalsIgnoreCase("CP1"))
                                    Utils.receiveEncryptedFile(fromServer, privateClientKey, "CP1",
                                            "download/");
                                else
                                    Utils.receiveEncryptedFile(fromServer, sessionKey, "CP2",
                                            "download/");
                            } else {
                                System.out.println("File does not exist in server!");
                            }
                        }
                    } else {
                        System.out.println("Please specify the file(s) to be downloaded!");
                    }
                    toServer.writeInt(PacketTypes.STOP_PACKET.getValue());
                } else if (command.equals("DEL")) {
                    if (files != null) {
                        for (String file : files) {
                            toServer.writeInt(PacketTypes.DELETE_FILE_PACKET.getValue());
                            byte[] encryptedFilename = ((mode.equalsIgnoreCase("CP1"))
                                    ? rsaKeyHelper.encryptWithPublic(file.getBytes())
                                    : aesCipherHelper.encryptWithKey(file.getBytes()));
                            toServer.writeInt(encryptedFilename.length);
                            toServer.write(encryptedFilename);
                            toServer.flush();
                            packetCount += 3;
                            int numBytes = fromServer.readInt();
                            byte[] buffer = new byte[numBytes];
                            fromServer.readFully(buffer, 0, numBytes);
                            System.out.println(new String(buffer, 0, numBytes));
                        }
                    } else {
                        System.out.println("Please specify the file(s) to be deleted!");
                    }
                    toServer.writeInt(PacketTypes.STOP_PACKET.getValue());
                } else if (command.equals("LSTDIR")) {
                    if (files == null) {
                        System.out.println("Directory listing: ");
                        toServer.writeInt(PacketTypes.LIST_DIRECTORY_PACKET.getValue());
                        packetCount++;
                        // Receive the number of files in the directory
                        int loopTimes = fromServer.readInt();
                        while (loopTimes > 0) {
                            int numBytes = fromServer.readInt();
                            byte[] encryptedFilename = new byte[numBytes];
                            fromServer.readFully(encryptedFilename, 0, numBytes);
                            byte[] decryptedFilename = ((mode.equalsIgnoreCase("CP1"))
                                    ? rsaKeyHelper.decryptWithPrivate(encryptedFilename)
                                    : aesCipherHelper.decryptWithKey(encryptedFilename));
                            System.out.println(
                                    new String(decryptedFilename, 0, decryptedFilename.length));
                            loopTimes--;
                        }
                    }
                    toServer.writeInt(PacketTypes.STOP_PACKET.getValue());
                } else if (command.equals("SHUTDOWN")) {
                    if (files == null) {
                        toServer.writeInt(PacketTypes.SHUTDOWN_PACKET.getValue());
                        packetCount++;
                        System.out.println("Shutting server down...");
                    }
                } else if (command.equals("HELP")) {
                    System.out.println(
                            "Please try either: UPLD <FILENAME>..., DWNLD <FILENAME>..., DEL <FILENAME>..., LSTDIR or SHUTDOWN.");
                } else {
                    System.out.println(
                            "Invalid command received. Please try either: UPLD <FILENAME>..., DWNLD <FILENAME>..., DEL <FILENAME>..., or LSTDIR.");
                }
            } catch (Exception e) {
                System.out.println("Connection error! Terminating client...");
            }
        }
        // Switch to interactive CLI shell mode
        else if (args[0].equalsIgnoreCase("SHELL")) {
            // Argument parsing section
            if (args.length > 1)
                serverAddress = args[1];

            if (args.length > 2)
                port = Integer.parseInt(args[2]);

            if (args.length > 3)
                mode = args[3];

            if (!mode.equalsIgnoreCase("CP1") && !mode.equalsIgnoreCase("CP2")) {
                System.out.println("Invalid mode specified!");
                System.exit(1);
            }

            if (args.length > 4)
                username = args[4];

            if (args.length > 5)
                password = args[5];

            System.out.println("Establishing connection to server...");

            try (Socket clientSocket = new Socket(serverAddress, port)) {
                // Get the input and output streams
                DataOutputStream toServer = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream fromServer = new DataInputStream(clientSocket.getInputStream());

                // Transmit mode
                toServer.writeInt(PacketTypes.CHANGE_MODE_PACKET.getValue());
                if (mode.equalsIgnoreCase("CP1"))
                    toServer.writeInt(PacketTypes.CP1_MODE_PACKET.getValue());
                else
                    toServer.writeInt(PacketTypes.CP2_MODE_PACKET.getValue());

                Scanner sc = null;

                // Prompt for login first, only if user never specifies username and password as
                // arguments
                if ((username == null) || (password == null)) {
                    // Do not close the scanner in this case, since we still need it later for the
                    // shell prompt
                    sc = new Scanner(System.in);
                    System.out.print("Username: ");
                    username = sc.nextLine();
                    System.out.print("Password: ");
                    password = sc.nextLine();
                }

                // Authenticate server and assign server's public key to variable
                int decryptedNonce = 0;
                System.out.println("Authenticating server...");
                try {
                    // Request authentication
                    toServer.writeInt(PacketTypes.VERIFY_SERVER_PACKET.getValue());
                    toServer.flush();
                    decryptedNonce = Utils.authenticate(nonce, toServer, fromServer,
                            "challenge_the_server", mode);
                } catch (Exception e) {
                    System.out.println("Authentication failed!");
                    e.printStackTrace();
                    clientSocket.close();
                    sc.close();
                    System.exit(1);
                }

                if (decryptedNonce != nonce) {
                    System.out.println("Failed to authenticate server!");
                    clientSocket.close();
                    sc.close();
                    System.exit(1);
                }

                System.out.println("Server is authenticated!");
                toServer.flush();

                PrivateKey privateClientKey = PrivateKeyReader.get(PRIVATE_KEY_FILENAME);
                RSAKeyHelper rsaKeyHelper = new RSAKeyHelper(publicServerKey, privateClientKey);

                int packetType = fromServer.readInt();
                if (packetType == PacketTypes.VERIFY_CLIENT_PACKET.getValue()) {
                    Utils.acceptChallenge(toServer, fromServer, privateClientKey,
                            PUBLIC_KEY_FILENAME);
                    toServer.flush();
                }

                // Verify user (separation of concerns from liveness check)
                try {
                    toServer.writeInt(PacketTypes.AUTH_LOGIN_USER_PACKET.getValue());
                    // Send username first for server to identify user
                    byte[] encryptedUsername = rsaKeyHelper.encryptWithPublic(username.getBytes());
                    toServer.writeInt(encryptedUsername.length);
                    toServer.write(encryptedUsername);
                    toServer.flush();
                    // Then send hash of concatenation of username and password
                    String hash = Utils.generateHash(username, password);
                    byte[] encryptedHash = rsaKeyHelper.encryptWithPublic(hash.getBytes());
                    toServer.writeInt(encryptedHash.length);
                    toServer.write(encryptedHash);
                    toServer.flush();
                } catch (RuntimeException e) {
                    System.out.println("Some unexpected behavior has occurred! Quitting...");
                    System.exit(1);
                }

                if (fromServer.readInt() != PacketTypes.OK_PACKET.getValue()) {
                    System.out.println("Wrong username or password!");
                    clientSocket.close();
                    System.exit(1);
                }

                System.out.println("Welcome, " + username + "!");

                // Still share session key with server, even if current mode is not CP2.
                // This allows the client and server to switch back and forth between CP1 and
                // CP2 independently without much fuss of attempting to transfer the session key
                // midway.
                Utils.doClientSessionKey(toServer, publicServerKey);
                AESCipherHelper aesCipherHelper = new AESCipherHelper(sessionKey);

                // Start file transfer
                while (true) {
                    // Read user input
                    System.out.print(">>> ");
                    if (sc == null) {
                        sc = new Scanner(System.in);
                    }
                    String input = sc.nextLine();
                    String[] inSplit = input.split(" ");

                    if (inSplit[0].equals("EXIT")) {
                        if (inSplit.length == 1) {
                            toServer.writeInt(PacketTypes.STOP_PACKET.getValue());
                            packetCount++;
                            System.out.println("Closing connection...");
                            sc.close();
                            break;
                        }
                    } else if (inSplit[0].equals("SHUTDOWN")) {
                        if (inSplit.length == 1) {
                            toServer.writeInt(PacketTypes.SHUTDOWN_PACKET.getValue());
                            packetCount++;
                            System.out.println("Closing connection and shutting server down...");
                            sc.close();
                            break;
                        }
                    } else if (inSplit[0].equals("UPLD")) {
                        String[] filesToSend = null;
                        if (inSplit.length < 2)
                            System.out.println("Please specify the file(s) to be uploaded!");
                        else {
                            filesToSend = Arrays.copyOfRange(inSplit, 1, inSplit.length);
                        }

                        if (filesToSend != null) {
                            try {
                                for (String file : filesToSend) {
                                    String filename = "data/" + file;
                                    toServer.writeInt(PacketTypes.UPLOAD_FILE_PACKET.getValue());
                                    int packets = ((mode.equalsIgnoreCase("CP1"))
                                            ? Utils.sendEncryptedFile(toServer, filename,
                                                    publicServerKey, "CP1")
                                            : Utils.sendEncryptedFile(toServer, filename,
                                                    sessionKey, "CP2"));
                                    packetCount += packets;
                                    int numBytes = fromServer.readInt();
                                    byte[] buffer = new byte[numBytes];
                                    fromServer.readFully(buffer, 0, numBytes);
                                    System.out.println(new String(buffer, 0, numBytes));
                                }
                            } catch (Exception e) {
                                System.out.println("Invalid file specified!");
                            }
                        }
                    } else if (inSplit[0].equals("DWNLD")) {
                        String[] filesToDownload = null;
                        if (inSplit.length < 2)
                            System.out.println("Please specify the file(s) to be downloaded!");
                        else {
                            filesToDownload = Arrays.copyOfRange(inSplit, 1, inSplit.length);
                        }

                        if (filesToDownload != null) {
                            for (String file : filesToDownload) {
                                toServer.writeInt(PacketTypes.DOWNLOAD_FILE_PACKET.getValue());
                                byte[] encryptedFilename = ((mode.equalsIgnoreCase("CP1"))
                                        ? rsaKeyHelper.encryptWithPublic(file.getBytes())
                                        : aesCipherHelper.encryptWithKey(file.getBytes()));
                                toServer.writeInt(encryptedFilename.length);
                                toServer.write(encryptedFilename);
                                toServer.flush();
                                packetCount += 3;
                                int fileExists = fromServer.readInt();
                                if (fileExists == PacketTypes.UPLOAD_FILE_PACKET.getValue()) {
                                    if (mode.equalsIgnoreCase("CP1"))
                                        Utils.receiveEncryptedFile(fromServer, privateClientKey,
                                                "CP1", "download/");
                                    else
                                        Utils.receiveEncryptedFile(fromServer, sessionKey, "CP2",
                                                "download/");
                                } else {
                                    System.out.println("File does not exist in server!");
                                }
                            }
                        }
                    } else if (inSplit[0].equals("DEL")) {
                        String[] filesToDel = null;
                        if (inSplit.length < 2)
                            System.out.println("Please specify the file(s) to be deleted!");
                        else {
                            filesToDel = Arrays.copyOfRange(inSplit, 1, inSplit.length);
                        }

                        if (filesToDel != null) {
                            for (String file : filesToDel) {
                                toServer.writeInt(PacketTypes.DELETE_FILE_PACKET.getValue());
                                byte[] encryptedFilename = ((mode.equalsIgnoreCase("CP1"))
                                        ? rsaKeyHelper.encryptWithPublic(file.getBytes())
                                        : aesCipherHelper.encryptWithKey(file.getBytes()));
                                toServer.writeInt(encryptedFilename.length);
                                toServer.write(encryptedFilename);
                                toServer.flush();
                                packetCount += 3;
                                int numBytes = fromServer.readInt();
                                byte[] buffer = new byte[numBytes];
                                fromServer.readFully(buffer, 0, numBytes);
                                System.out.println(new String(buffer, 0, numBytes));
                            }
                        }
                    } else if (inSplit[0].equals("LSTDIR")) {
                        if (inSplit.length == 1) {
                            System.out.println("Directory listing: ");
                            toServer.writeInt(PacketTypes.LIST_DIRECTORY_PACKET.getValue());
                            packetCount++;
                            // Receive the number of files in the directory
                            int loopTimes = fromServer.readInt();
                            while (loopTimes > 0) {
                                int numBytes = fromServer.readInt();
                                byte[] encryptedFilename = new byte[numBytes];
                                fromServer.readFully(encryptedFilename, 0, numBytes);
                                byte[] decryptedFilename = ((mode.equalsIgnoreCase("CP1"))
                                        ? rsaKeyHelper.decryptWithPrivate(encryptedFilename)
                                        : aesCipherHelper.decryptWithKey(encryptedFilename));
                                System.out.println(
                                        new String(decryptedFilename, 0, decryptedFilename.length));
                                loopTimes--;
                            }
                        }
                    } else if (inSplit[0].equals("HELP")) {
                        System.out.println(
                                "Please try either: EXIT, UPLD <FILENAME>..., DWNLD <FILENAME>..., DEL <FILENAME>..., LSTDIR or SHUTDOWN.");
                    } else {
                        System.out.println(
                                "Invalid command received. Please try either: EXIT, UPLD <FILENAME>..., DWNLD <FILENAME>..., DEL <FILENAME>..., LSTDIR or SHUTDOWN.");
                    }
                }
            } catch (Exception e) {
                System.out.println("Connection error! Terminating client...");
                e.printStackTrace();
            }
        }
        // Switch to GUI mode
        else if (args[0].equalsIgnoreCase("GUI")) {
            // Check if current device is on headless mode
            if (GraphicsEnvironment.isHeadless()) {
                System.out.println(
                        "Current device is on headless mode! Please disable headless mode or use this mode of operation only on a device equipped with and capable of an interactive environment!");
                System.exit(1);
            }

            // JFrame GUI setup (this allows for multi-monitor configurations)
            GraphicsDevice gd =
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            int width = gd.getDisplayMode().getWidth();
            int height = gd.getDisplayMode().getHeight();

            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                System.out.println("Unexpected behavior has been encountered!");
                System.exit(1);
            }

            UIManager.getLookAndFeelDefaults().put("defaultFont",
                    new FontUIResource(new Font(Font.MONOSPACED, Font.PLAIN, 20)));

            // Main GUI service provider.
            // We use Swing since we are just using the built-in native pure Java JDK
            // libraries (instead of using external dependencies like JavaFX, SWT, Qt Jambi,
            // JGoodies, Apache Pivot, etc.)
            // Also, Swingx is ded.
            // Compared to AWT, Swing is better in many aspects.
            // We use an event-dispatching thread to invoke the GUI setup code for thread safety
            // purposes.
            // There is a lot of boilerplate code here, so just skip this part and move on.
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("FTPS Client With Security");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.setLocationByPlatform(true);
                Container contentPane = frame.getContentPane();
                // We use a layout manager to make all components somewhat responsive/reactive to
                // window resizing
                GridBagLayout layout = new GridBagLayout();
                contentPane.setLayout(layout);
                // We can re-use the same constraints object, as long as we remember to re-set
                // everything again every time we add one component to the container panel
                GridBagConstraints c = new GridBagConstraints();
                JLabel serverIPAddressLabel = new JLabel("Server IP Address");
                c.fill = GridBagConstraints.HORIZONTAL; // Natural height, maximum width
                c.weightx = 0.5;
                c.weighty = 0.0;
                c.gridx = 0;
                c.gridy = 0;
                c.insets = new Insets(10, 10, 10, 10);
                // Add component to main JFrame's content pane container with specified constraints
                contentPane.add(serverIPAddressLabel, c);
                JTextField serverIPAddressField = new JTextField("localhost");
                serverIPAddressField.setToolTipText("Enter the server's IPv4 address here.");
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.5;
                c.weighty = 0.0;
                c.gridx = 1;
                c.gridy = 0;
                c.insets = new Insets(10, 10, 10, 10);
                contentPane.add(serverIPAddressField, c);
                JLabel serverPortLabel = new JLabel("Server Port");
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.5;
                c.weighty = 0.0;
                c.gridx = 0;
                c.gridy = 1;
                c.insets = new Insets(10, 10, 10, 10);
                contentPane.add(serverPortLabel, c);
                SpinnerModel serverPortSpinnerModel = new SpinnerNumberModel(4321, 0, 65535, 1);
                JSpinner serverPortSpinner = new JSpinner(serverPortSpinnerModel);
                serverPortSpinner.setToolTipText("Enter the server's socket port number here.");
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.5;
                c.weighty = 0.0;
                c.gridx = 1;
                c.gridy = 1;
                c.insets = new Insets(10, 10, 10, 10);
                contentPane.add(serverPortSpinner, c);
                JLabel modeLabel = new JLabel("CP Mode");
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.5;
                c.weighty = 0.0;
                c.gridx = 0;
                c.gridy = 2;
                c.insets = new Insets(10, 10, 10, 10);
                contentPane.add(modeLabel, c);
                JRadioButton cp1Button = new JRadioButton("CP1");
                JRadioButton cp2Button = new JRadioButton("CP2");
                cp1Button.setMnemonic(KeyEvent.VK_1);
                cp2Button.setMnemonic(KeyEvent.VK_2);
                cp1Button.setActionCommand("CP1");
                cp2Button.setActionCommand("CP2");
                cp1Button.setToolTipText(
                        "Click this button to select and use RSA-4096 encryption method.");
                cp2Button.setToolTipText(
                        "Click this button to select and use AES-256 encryption method.");
                cp2Button.setSelected(true); // Set CP2 as default
                // This button group is logical, not physical
                ButtonGroup cpGroup = new ButtonGroup();
                cpGroup.add(cp1Button);
                cpGroup.add(cp2Button);
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.25;
                c.weighty = 0.0;
                c.gridx = 1;
                c.gridy = 2;
                c.insets = new Insets(10, 5, 10, 5);
                contentPane.add(cp1Button, c);
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.25;
                c.weighty = 0.0;
                c.gridx = 2;
                c.gridy = 2;
                c.insets = new Insets(10, 5, 10, 5);
                contentPane.add(cp2Button, c);
                JLabel usernameLabel = new JLabel("Username");
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.5;
                c.weighty = 0.0;
                c.gridx = 0;
                c.gridy = 3;
                c.insets = new Insets(10, 10, 10, 10);
                contentPane.add(usernameLabel, c);
                JTextField usernameField = new JTextField();
                usernameField.setToolTipText("Enter your username here.");
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.5;
                c.weighty = 0.0;
                c.gridx = 1;
                c.gridy = 3;
                c.insets = new Insets(10, 10, 10, 10);
                contentPane.add(usernameField, c);
                JLabel passwordLabel = new JLabel("Password");
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.5;
                c.weighty = 0.0;
                c.gridx = 0;
                c.gridy = 4;
                c.insets = new Insets(10, 10, 10, 10);
                contentPane.add(passwordLabel, c);
                JPasswordField passwordField = new JPasswordField();
                passwordField.setToolTipText("Enter your password here.");
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.5;
                c.weighty = 0.0;
                c.gridx = 1;
                c.gridy = 4;
                c.insets = new Insets(10, 10, 10, 10);
                contentPane.add(passwordField, c);
                JLabel commandLabel = new JLabel("Desired Command");
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.5;
                c.weighty = 0.0;
                c.gridx = 0;
                c.gridy = 5;
                c.insets = new Insets(10, 10, 10, 10);
                contentPane.add(commandLabel, c);
                String[] commands = {"UPLD", "DWNLD", "LSTDIR", "DEL", "SHUTDOWN"};
                SpinnerModel commandSpinnerModel = new SpinnerListModel(commands);
                JSpinner commandSpinner = new JSpinner(commandSpinnerModel);
                commandSpinner.setToolTipText("Select the command that you want to execute.");
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.5;
                c.weighty = 0.0;
                c.gridx = 1;
                c.gridy = 5;
                c.insets = new Insets(10, 10, 10, 10);
                contentPane.add(commandSpinner, c);
                // Set up file chooser (limit to only the `data/` directory)
                File root = new File("data");
                FileSystemView fsv = new SingleRootFileSystemView(root);
                JFileChooser chooser = new JFileChooser(fsv);
                chooser.setMultiSelectionEnabled(true); // Enable selection of multiple files
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setControlButtonsAreShown(false);
                c.fill = GridBagConstraints.HORIZONTAL;
                c.ipady = 40;
                c.weightx = 1.0;
                c.weighty = 0.5;
                c.gridx = 0;
                c.gridwidth = 2;
                c.gridy = 6;
                c.insets = new Insets(10, 10, 10, 10);
                contentPane.add(chooser, c);
                JButton resetButton = new JButton("Reset Fields");
                resetButton.setToolTipText(
                        "Click this button to reset all fields to their default values.");
                c.fill = GridBagConstraints.HORIZONTAL;
                c.ipady = 0;
                c.weightx = 0.5;
                c.weighty = 0.0;
                c.gridx = 0;
                c.gridwidth = 1;
                c.gridy = 7;
                c.insets = new Insets(10, 10, 10, 10);
                contentPane.add(resetButton, c);
                JButton executeButton = new JButton("Execute Connection");
                executeButton.setToolTipText(
                        "Click this button to initiate connection with server and execute the selected command.");
                c.fill = GridBagConstraints.HORIZONTAL;
                c.ipady = 0;
                c.weightx = 0.5;
                c.weighty = 0.0;
                c.gridx = 1;
                c.gridwidth = 1;
                c.gridy = 7;
                c.insets = new Insets(10, 10, 10, 10);
                contentPane.add(executeButton, c);
                JTextArea logs = new JTextArea();
                JScrollPane pane = new JScrollPane(logs);
                c.fill = GridBagConstraints.HORIZONTAL;
                c.ipady = 120;
                c.weightx = 1;
                c.weighty = 0.5;
                c.gridx = 0;
                c.gridwidth = 2;
                c.gridy = 8;
                c.insets = new Insets(10, 10, 10, 10);
                c.anchor = GridBagConstraints.PAGE_END;
                contentPane.add(pane, c);

                // Update file chooser UI depending on the command chosen
                commandSpinner.addChangeListener(e -> {
                    JSpinner spinner = (JSpinner) e.getSource();
                    String value = (String) spinner.getValue();
                    GridBagConstraints newC = new GridBagConstraints();
                    if (value.equalsIgnoreCase("UPLD")) {
                        Component oldChooser = null;
                        for (Component comp : contentPane.getComponents()) {
                            GridBagConstraints gbc = layout.getConstraints(comp);
                            if (gbc.gridx == 0 && gbc.gridy == 6) {
                                oldChooser = comp;
                                break;
                            }
                        }
                        contentPane.remove(oldChooser);
                        File newRoot = new File("data");
                        FileSystemView newFsv = new SingleRootFileSystemView(newRoot);
                        JFileChooser newChooser = new JFileChooser(newFsv);
                        newChooser.setMultiSelectionEnabled(true);
                        newChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        newChooser.setControlButtonsAreShown(false);
                        newC.fill = GridBagConstraints.HORIZONTAL;
                        newC.ipady = 40;
                        newC.weightx = 1.0;
                        newC.weighty = 0.5;
                        newC.gridx = 0;
                        newC.gridwidth = 2;
                        newC.gridy = 6;
                        newC.insets = new Insets(10, 10, 10, 10);
                        contentPane.add(newChooser, newC);
                        // These are best practices
                        contentPane.revalidate();
                        contentPane.repaint();
                    } else if (value.equalsIgnoreCase("DWNLD") || value.equalsIgnoreCase("DEL")) {
                        Component oldChooser = null;
                        for (Component comp : contentPane.getComponents()) {
                            GridBagConstraints gbc = layout.getConstraints(comp);
                            if (gbc.gridx == 0 && gbc.gridy == 6) {
                                oldChooser = comp;
                                break;
                            }
                        }
                        contentPane.remove(oldChooser);
                        // TODO: This direct access should only be a temporary patch/fix, since it
                        // will not work for remote servers
                        File newRoot = new File("upload");
                        FileSystemView newFsv = new SingleRootFileSystemView(newRoot);
                        JFileChooser newChooser = new JFileChooser(newFsv);
                        newChooser.setMultiSelectionEnabled(true);
                        newChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        newChooser.setControlButtonsAreShown(false);
                        newC.fill = GridBagConstraints.HORIZONTAL;
                        newC.ipady = 40;
                        newC.weightx = 1.0;
                        newC.weighty = 0.5;
                        newC.gridx = 0;
                        newC.gridwidth = 2;
                        newC.gridy = 6;
                        newC.insets = new Insets(10, 10, 10, 10);
                        contentPane.add(newChooser, newC);
                        // These are best practices
                        contentPane.revalidate();
                        contentPane.repaint();
                    }
                });

                // Set up listener to reset all fields to their default values
                resetButton.addActionListener(e -> {
                    serverIPAddressField.setText("localhost");
                    serverPortSpinner.setValue(Integer.valueOf(4321));
                    cp2Button.setSelected(true);
                    usernameField.setText("");
                    passwordField.setText("");
                    commandSpinner.setValue("UPLD");
                });

                // Set up main listener to execute connection and communicate with server using the
                // collected data from GUI
                executeButton.addActionListener(e -> {
                    String serverIPAddress = serverIPAddressField.getText();
                    int serverPortNumber = (Integer) serverPortSpinner.getValue();
                    mode = cpGroup.getSelection().getActionCommand();
                    username = usernameField.getText();
                    password = String.valueOf(passwordField.getPassword());
                    String command = (String) commandSpinner.getValue();
                    JFileChooser usedChooser = null;
                    for (Component comp : contentPane.getComponents()) {
                        GridBagConstraints gbc = layout.getConstraints(comp);
                        if (gbc.gridx == 0 && gbc.gridy == 6) {
                            usedChooser = (JFileChooser) comp;
                            break;
                        }
                    }
                    File[] files = usedChooser.getSelectedFiles();
                    Utils.addTextToScrollableTextArea(logs, "Establishing connection to server...");

                    // Attempt to connect to server
                    try (Socket clientSocket = new Socket(serverIPAddress, serverPortNumber)) {
                        // Get the input and output streams
                        DataOutputStream toServer =
                                new DataOutputStream(clientSocket.getOutputStream());
                        DataInputStream fromServer =
                                new DataInputStream(clientSocket.getInputStream());

                        // Transmit mode
                        toServer.writeInt(PacketTypes.CHANGE_MODE_PACKET.getValue());
                        if (mode.equalsIgnoreCase("CP1"))
                            toServer.writeInt(PacketTypes.CP1_MODE_PACKET.getValue());
                        else
                            toServer.writeInt(PacketTypes.CP2_MODE_PACKET.getValue());

                        // Authenticate server and assign server's public key to variable
                        int decryptedNonce = 0;
                        Utils.addTextToScrollableTextArea(logs, "Authenticating server...");
                        try {
                            // Request authentication
                            toServer.writeInt(PacketTypes.VERIFY_SERVER_PACKET.getValue());
                            toServer.flush();
                            decryptedNonce = Utils.authenticate(nonce, toServer, fromServer,
                                    "challenge_the_server", mode);
                        } catch (Exception exc) {
                            Utils.addTextToScrollableTextArea(logs, "Authentication failed!");
                            clientSocket.close();
                            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                            System.exit(1);
                        }

                        if (decryptedNonce != nonce) {
                            Utils.addTextToScrollableTextArea(logs,
                                    "Failed to authenticate server!");
                            clientSocket.close();
                            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                            System.exit(1);
                        }

                        Utils.addTextToScrollableTextArea(logs, "Server is authenticated!");

                        PrivateKey privateClientKey = PrivateKeyReader.get(PRIVATE_KEY_FILENAME);
                        RSAKeyHelper rsaKeyHelper =
                                new RSAKeyHelper(publicServerKey, privateClientKey);

                        int packetType = fromServer.readInt();
                        if (packetType == PacketTypes.VERIFY_CLIENT_PACKET.getValue()) {
                            Utils.acceptChallenge(toServer, fromServer, privateClientKey,
                                    PUBLIC_KEY_FILENAME);
                        }

                        // Verify user (separation of concerns from liveness check)
                        try {
                            toServer.writeInt(PacketTypes.AUTH_LOGIN_USER_PACKET.getValue());
                            // Send username first for server to identify user
                            byte[] encryptedUsername =
                                    rsaKeyHelper.encryptWithPublic(username.getBytes());
                            toServer.writeInt(encryptedUsername.length);
                            toServer.write(encryptedUsername);
                            toServer.flush();
                            // Then send hash of concatenation of username and password
                            String hash = Utils.generateHash(username, password);
                            byte[] encryptedHash = rsaKeyHelper.encryptWithPublic(hash.getBytes());
                            toServer.writeInt(encryptedHash.length);
                            toServer.write(encryptedHash);
                            toServer.flush();
                        } catch (RuntimeException exc) {
                            Utils.addTextToScrollableTextArea(logs,
                                    "Some unexpected behavior has occurred! Quitting...");
                            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                            System.exit(1);
                        }

                        if (fromServer.readInt() != PacketTypes.OK_PACKET.getValue()) {
                            Utils.addTextToScrollableTextArea(logs, "Wrong username or password!");
                            clientSocket.close();
                            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                            System.exit(1);
                        }

                        Utils.addTextToScrollableTextArea(logs, "Welcome, " + username + "!");

                        // Still share session key with server, even if current mode is not CP2.
                        // This allows the client and server to switch back and forth between CP1
                        // and CP2 independently without much fuss of attempting to transfer the
                        // session key midway.
                        Utils.doClientSessionKey(toServer, publicServerKey);
                        AESCipherHelper aesCipherHelper = new AESCipherHelper(sessionKey);

                        if (command.equals("UPLD")) {
                            if (files.length > 0) {
                                try {
                                    // This overly-complicated solution is here for us to be able to
                                    // get the relative paths of the files
                                    Path cwd = Paths.get(new File(System.getProperty("user.dir"))
                                            .getCanonicalPath());
                                    for (File file : files) {
                                        Path absPath = Paths.get(file.getCanonicalPath());
                                        String filename = cwd.relativize(absPath).toString();
                                        toServer.writeInt(
                                                PacketTypes.UPLOAD_FILE_PACKET.getValue());
                                        int packets = ((mode.equalsIgnoreCase("CP1"))
                                                ? Utils.sendEncryptedFile(toServer, filename,
                                                        publicServerKey, "CP1")
                                                : Utils.sendEncryptedFile(toServer, filename,
                                                        sessionKey, "CP2"));
                                        int numBytes = fromServer.readInt();
                                        byte[] buffer = new byte[numBytes];
                                        fromServer.readFully(buffer, 0, numBytes);
                                        Utils.addTextToScrollableTextArea(logs,
                                                new String(buffer, 0, numBytes));
                                    }
                                } catch (IOException exc) {
                                    Utils.addTextToScrollableTextArea(logs,
                                            "Unexpected behavior has been encountered!");
                                }
                            } else {
                                Utils.addTextToScrollableTextArea(logs,
                                        "Please specify the file(s) to be uploaded!");
                            }
                            toServer.writeInt(PacketTypes.STOP_PACKET.getValue());
                        } else if (command.equals("DWNLD")) {
                            if (files.length > 0) {
                                try {
                                    // This overly-complicated solution is here for us to be able to
                                    // get the relative paths of the files
                                    Path cwd = Paths.get(new File(System.getProperty("user.dir"))
                                            .getCanonicalPath() + "/upload");
                                    for (File file : files) {
                                        Path absPath = Paths.get(file.getCanonicalPath());
                                        String filename = cwd.relativize(absPath).toString();
                                        toServer.writeInt(
                                                PacketTypes.DOWNLOAD_FILE_PACKET.getValue());
                                        byte[] encryptedFilename = ((mode.equalsIgnoreCase("CP1"))
                                                ? rsaKeyHelper
                                                        .encryptWithPublic(filename.getBytes())
                                                : aesCipherHelper
                                                        .encryptWithKey(filename.getBytes()));
                                        toServer.writeInt(encryptedFilename.length);
                                        toServer.write(encryptedFilename);
                                        toServer.flush();
                                        int fileExists = fromServer.readInt();
                                        if (fileExists == PacketTypes.UPLOAD_FILE_PACKET
                                                .getValue()) {
                                            if (mode.equalsIgnoreCase("CP1"))
                                                Utils.receiveEncryptedFile(fromServer,
                                                        privateClientKey, "CP1", "download/");
                                            else
                                                Utils.receiveEncryptedFile(fromServer, sessionKey,
                                                        "CP2", "download/");
                                        } else {
                                            Utils.addTextToScrollableTextArea(logs,
                                                    "File does not exist in server!");
                                        }
                                    }
                                } catch (IOException exc) {
                                    Utils.addTextToScrollableTextArea(logs,
                                            "Unexpected behavior has been encountered!");
                                }
                            } else {
                                Utils.addTextToScrollableTextArea(logs,
                                        "Please specify the file(s) to be downloaded!");
                            }
                            toServer.writeInt(PacketTypes.STOP_PACKET.getValue());
                        } else if (command.equals("DEL")) {
                            if (files.length > 0) {
                                try {
                                    // This overly-complicated solution is here for us to be able to
                                    // get the relative paths of the files
                                    Path cwd = Paths.get(new File(System.getProperty("user.dir"))
                                            .getCanonicalPath() + "/upload");
                                    for (File file : files) {
                                        Path absPath = Paths.get(file.getAbsolutePath());
                                        String filename = cwd.relativize(absPath).toString();
                                        toServer.writeInt(
                                                PacketTypes.DELETE_FILE_PACKET.getValue());
                                        byte[] encryptedFilename = ((mode.equalsIgnoreCase("CP1"))
                                                ? rsaKeyHelper
                                                        .encryptWithPublic(filename.getBytes())
                                                : aesCipherHelper
                                                        .encryptWithKey(filename.getBytes()));
                                        toServer.writeInt(encryptedFilename.length);
                                        toServer.write(encryptedFilename);
                                        toServer.flush();
                                        int numBytes = fromServer.readInt();
                                        byte[] buffer = new byte[numBytes];
                                        fromServer.readFully(buffer, 0, numBytes);
                                        Utils.addTextToScrollableTextArea(logs,
                                                new String(buffer, 0, numBytes));
                                    }
                                    contentPane.remove(usedChooser);
                                    GridBagConstraints newC = new GridBagConstraints();
                                    // TODO: This direct access should only be a temporary
                                    // patch/fix, since it will not work for remote servers
                                    File newRoot = new File("upload");
                                    FileSystemView newFsv = new SingleRootFileSystemView(newRoot);
                                    JFileChooser newChooser = new JFileChooser(newFsv);
                                    newChooser.setMultiSelectionEnabled(true);
                                    newChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                                    newChooser.setControlButtonsAreShown(false);
                                    newC.fill = GridBagConstraints.HORIZONTAL;
                                    newC.ipady = 40;
                                    newC.weightx = 1.0;
                                    newC.weighty = 0.5;
                                    newC.gridx = 0;
                                    newC.gridwidth = 2;
                                    newC.gridy = 6;
                                    newC.insets = new Insets(10, 10, 10, 10);
                                    contentPane.add(newChooser, newC);
                                    // These are best practices
                                    contentPane.revalidate();
                                    contentPane.repaint();
                                } catch (IOException exc) {
                                    Utils.addTextToScrollableTextArea(logs,
                                            "Unexpected behavior has been encountered!");
                                }
                            } else {
                                Utils.addTextToScrollableTextArea(logs,
                                        "Please specify the file(s) to be deleted!");
                            }
                            toServer.writeInt(PacketTypes.STOP_PACKET.getValue());
                        } else if (command.equals("LSTDIR")) {
                            Utils.addTextToScrollableTextArea(logs, "Directory listing: ");
                            toServer.writeInt(PacketTypes.LIST_DIRECTORY_PACKET.getValue());
                            // Receive the number of files in the directory
                            int loopTimes = fromServer.readInt();
                            while (loopTimes > 0) {
                                int numBytes = fromServer.readInt();
                                byte[] encryptedFilename = new byte[numBytes];
                                fromServer.readFully(encryptedFilename, 0, numBytes);
                                byte[] decryptedFilename = ((mode.equalsIgnoreCase("CP1"))
                                        ? rsaKeyHelper.decryptWithPrivate(encryptedFilename)
                                        : aesCipherHelper.decryptWithKey(encryptedFilename));
                                Utils.addTextToScrollableTextArea(logs,
                                        new String(decryptedFilename, 0, decryptedFilename.length));
                                loopTimes--;
                            }
                            toServer.writeInt(PacketTypes.STOP_PACKET.getValue());
                        } else if (command.equals("SHUTDOWN")) {
                            toServer.writeInt(PacketTypes.SHUTDOWN_PACKET.getValue());
                            Utils.addTextToScrollableTextArea(logs, "Shutting server down...");
                            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                        } else {
                            Utils.addTextToScrollableTextArea(logs,
                                    "Invalid command received. Please try either: UPLD <FILENAME>..., DWNLD <FILENAME>..., DEL <FILENAME>..., or LSTDIR.");
                        }
                    } catch (Exception exc) {
                        System.out.println("Connection error! Ensure that the details entered are correct and that the server is currently running!");
                        Utils.addTextToScrollableTextArea(logs,
                                "Connection error! Ensure that the details entered are correct and that the server is currently running!");
                    }
                });

                // Listen to when the window is closed (this will require and generate an anonymous
                // inner class)
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        long timeTaken = System.nanoTime() - timeStarted;
                        System.out
                                .println("Program took: " + timeTaken / 1000000.0 + " ms to run.");
                        if (mode.equalsIgnoreCase("CP1"))
                            System.out.println("The method used was RSA.");
                        else
                            System.out.println("The method used was AES.");
                        e.getWindow().dispose(); // Close GUI window
                    }
                });

                // Automatically set the components (frame's contents) to their preferred sizes
                frame.pack();

                // Show GUI window on display monitor
                frame.setVisible(true);
            });
        }
        // Catch invalid modes of operation
        else {
            System.out.println(
                    "Unimplemented mode of operation! Please specify an available mode of operation: CLI, SHELL or GUI.");
            System.exit(1);
        }

        if (args[0].equalsIgnoreCase("CLI") || args[0].equalsIgnoreCase("SHELL")) {
            long timeTaken = System.nanoTime() - timeStarted;
            System.out.println("Program took: " + timeTaken / 1000000.0 + " ms to run.");
            System.out.println("Number of meaningful packets sent: " + packetCount);
            if (mode.equalsIgnoreCase("CP1"))
                System.out.println("The method used was RSA.");
            else
                System.out.println("The method used was AES.");
        }
    }
}
