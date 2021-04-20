import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Scanner;

public class ClientCP1 {
    private static String privateKeyFilename = "credentials/client/private_key.der";
    private static String publicKeyFilename = "credentials/client/public_key.der";
    public static PublicKey publicServerKey;
    private static String username;
    private static String password;

    public static void main(String[] args) {
        // Initial setup
        Utils.makeFolder("download");

        String serverAddress = "localhost";
        int port = 4321;

        // Generate client-side nonce to verify that client is talking to a live server
        final int nonce = new SecureRandom().nextInt();

        // TODO: Generate client's sequence number to prevent partial replay/playback attacks
        int sequence = new SecureRandom().nextInt();
        int packetCount = 0;

        // Argument parsing section
        if (args.length > 0)
            serverAddress = args[0];

        if (args.length > 1)
            port = Integer.parseInt(args[1]);

        // Start timer
        long timeStarted = System.nanoTime();

        // Switch to command-line arguments mode
        if (args.length > 2) {
            System.out.println("Establishing connection to server...");

            // Attempt to connect to server
            try (Socket clientSocket = new Socket(serverAddress, port)) {
                // Get the input and output streams
                DataOutputStream toServer = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream fromServer = new DataInputStream(clientSocket.getInputStream());

                // Prompt for login first
                Scanner sc = new Scanner(System.in);
                System.out.print("Username: ");
                username = sc.nextLine();
                System.out.print("Password: ");
                password = sc.nextLine();
                sc.close();

                // Authenticate server and assign server's public key to variable
                int decryptedNonce = 0;
                System.out.println("Authenticating server...");
                try {
                    decryptedNonce = Utils.authenticate(nonce, toServer, fromServer,
                            "challenge_the_server", "CP1");
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

                PrivateKey privateClientKey = PrivateKeyReader.get(privateKeyFilename);
                RSAKeyHelper rsaKeyHelper = new RSAKeyHelper(publicServerKey, privateClientKey);

                int packetType = fromServer.readInt();
                if (packetType == PacketTypes.VERIFY_CLIENT_PACKET.getValue()) {
                    Utils.acceptChallenge(toServer, fromServer, privateClientKey,
                            publicKeyFilename);
                }

                // For some reason, putting this prevents a deadlock.
                // TODO: Fix protocol!
                Thread.sleep(1000);

                // Verify user
                toServer.writeInt(PacketTypes.AUTH_LOGIN_USERNAME_PACKET.getValue());
                byte[] encryptedUsername = rsaKeyHelper.encryptWithPublic(username.getBytes());
                toServer.writeInt(encryptedUsername.length);
                toServer.write(encryptedUsername);
                toServer.flush();
                if (fromServer.readInt() == PacketTypes.OK_PACKET.getValue()) {
                    toServer.writeInt(PacketTypes.AUTH_LOGIN_PASSWORD_PACKET.getValue());
                    byte[] encryptedPassword = rsaKeyHelper.encryptWithPublic(password.getBytes());
                    toServer.writeInt(encryptedPassword.length);
                    toServer.write(encryptedPassword);
                    toServer.flush();

                    int anotherPacketType = fromServer.readInt();

                    if (anotherPacketType != PacketTypes.OK_PACKET.getValue()) {
                        System.out.println("Wrong password!");
                        clientSocket.close();
                        sc.close();
                        System.exit(1);
                    }
                } else {
                    System.out.println("Wrong username!");
                    clientSocket.close();
                    sc.close();
                    System.exit(1);
                }

                System.out.println("Welcome, " + username + "!");

                String command = args[2];
                String[] files = null;

                // Grab all the filenames from the rest of the command-line arguments
                if (args.length > 3) {
                    files = Arrays.copyOfRange(args, 3, args.length);
                }

                if (command.equals("UPLD")) {
                    if (files != null) {
                        try {
                            for (String file : files) {
                                String filename = "data/" + file;
                                int packets = Utils.sendEncryptedFile(toServer, filename,
                                        publicServerKey, "CP1");
                                packetCount += packets;
                            }
                        } catch (Exception e) {
                            System.out.println("Invalid file specified!");
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Please specify the file(s) to be uploaded!");
                    }
                } else if (command.equals("DWNLD")) {
                    if (files != null) {
                        for (String file : files) {
                            toServer.writeInt(PacketTypes.DOWNLOAD_FILE_PACKET.getValue());
                            byte[] encryptedFilename =
                                    rsaKeyHelper.encryptWithPublic(file.getBytes());
                            toServer.writeInt(encryptedFilename.length);
                            toServer.write(encryptedFilename);
                            toServer.flush();
                            packetCount += 3;
                            int fileExists = fromServer.readInt();
                            if (fileExists == PacketTypes.UPLOAD_FILE_PACKET.getValue()) {
                                Utils.receiveEncryptedFile(fromServer, privateClientKey, "CP1",
                                        "download/");
                            } else {
                                System.out.println("File does not exist in server!");
                            }
                        }
                    } else {
                        System.out.println("Please specify the file(s) to be downloaded!");
                    }
                } else if (command.equals("DEL")) {
                    if (files != null) {
                        for (String file : files) {
                            toServer.writeInt(PacketTypes.DELETE_FILE_PACKET.getValue());
                            byte[] encryptedFilename =
                                    rsaKeyHelper.encryptWithPublic(file.getBytes());
                            toServer.writeInt(encryptedFilename.length);
                            toServer.write(encryptedFilename);
                            toServer.flush();
                            packetCount += 3;
                            int numBytes = fromServer.readInt();
                            byte[] buffer = new byte[numBytes];
                            fromServer.readFully(buffer, 0, numBytes);
                            System.out.println(
                                    new String(buffer, 0, numBytes) + " has been deleted!");
                        }
                    } else {
                        System.out.println("Please specify the file(s) to be deleted!");
                    }
                } else if (command.equals("LSTDIR")) {
                    if (files == null) {
                        toServer.writeInt(PacketTypes.LIST_DIRECTORY_PACKET.getValue());
                        packetCount++;
                        // Receive the number of files in the directory
                        int loopTimes = fromServer.readInt();
                        while (loopTimes > 0) {
                            int numBytes = fromServer.readInt();
                            byte[] encryptedFilename = new byte[numBytes];
                            fromServer.readFully(encryptedFilename, 0, numBytes);
                            byte[] decryptedFilename =
                                    rsaKeyHelper.decryptWithPrivate(encryptedFilename);
                            System.out.println(
                                    new String(decryptedFilename, 0, decryptedFilename.length));
                            loopTimes--;
                        }
                    }
                } else {
                    System.out.println(
                            "Invalid command received. Please try either: UPLD <FILENAME>, DWNLD <FILENAME>, DEL <FILENAME>, or LSTDIR.");
                }
            } catch (Exception e) {
                System.out.println("Connection error! Terminating client...");
            }
        }
        // Switch to interactive shell mode
        else {
            try (Socket clientSocket = new Socket(serverAddress, port)) {
                // Get the input and output streams
                DataOutputStream toServer = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream fromServer = new DataInputStream(clientSocket.getInputStream());

                // Prompt for login first (do not close this scanner since we need to use it later
                // to read input commands)
                // Only one scanner should be used in the same program
                Scanner sc = new Scanner(System.in);
                System.out.print("Username: ");
                username = sc.nextLine();
                System.out.print("Password: ");
                password = sc.nextLine();

                // Authenticate server and assign server's public key to variable
                int decryptedNonce = 0;
                System.out.println("Authenticating server...");
                try {
                    decryptedNonce = Utils.authenticate(nonce, toServer, fromServer,
                            "challenge_the_server", "CP1");
                } catch (Exception e) {
                    System.out.println("Authentication failed!");
                    e.printStackTrace();
                    sc.close();
                    clientSocket.close();
                    System.exit(1);
                }

                if (decryptedNonce != nonce) {
                    System.out.println("Failed to authenticate server!");
                    sc.close();
                    clientSocket.close();
                    System.exit(1);
                }

                System.out.println("Server is authenticated!");
                toServer.flush();

                PrivateKey privateClientKey = PrivateKeyReader.get(privateKeyFilename);
                RSAKeyHelper rsaKeyHelper = new RSAKeyHelper(publicServerKey, privateClientKey);

                int packetType = fromServer.readInt();
                if (packetType == PacketTypes.VERIFY_CLIENT_PACKET.getValue()) {
                    Utils.acceptChallenge(toServer, fromServer, privateClientKey,
                            publicKeyFilename);
                    toServer.flush();
                }

                // For some reason, putting this prevents a deadlock.
                // TODO: Fix protocol!
                Thread.sleep(1000);

                // Verify user
                toServer.writeInt(PacketTypes.AUTH_LOGIN_USERNAME_PACKET.getValue());
                byte[] encryptedUsername = rsaKeyHelper.encryptWithPublic(username.getBytes());
                toServer.writeInt(encryptedUsername.length);
                toServer.write(encryptedUsername);
                toServer.flush();
                if (fromServer.readInt() == PacketTypes.OK_PACKET.getValue()) {
                    toServer.writeInt(PacketTypes.AUTH_LOGIN_PASSWORD_PACKET.getValue());
                    byte[] encryptedPassword = rsaKeyHelper.encryptWithPublic(password.getBytes());
                    toServer.writeInt(encryptedPassword.length);
                    toServer.write(encryptedPassword);
                    toServer.flush();

                    int anotherPacketType = fromServer.readInt();

                    if (anotherPacketType != PacketTypes.OK_PACKET.getValue()) {
                        System.out.println("Wrong password!");
                        clientSocket.close();
                        sc.close();
                        System.exit(1);
                    }
                } else {
                    System.out.println("Wrong username!");
                    clientSocket.close();
                    sc.close();
                    System.exit(1);
                }

                System.out.println("Welcome, " + username + "!");

                // Start file transfer
                while (true) {
                    // Read user input
                    System.out.print(">>> ");
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
                                    int packets = Utils.sendEncryptedFile(toServer, filename,
                                            publicServerKey, "CP1");
                                    packetCount += packets;
                                    int numBytes = fromServer.readInt();
                                    byte[] buffer = new byte[numBytes];
                                    fromServer.readFully(buffer, 0, numBytes);
                                    System.out.println(new String(buffer, 0, numBytes));
                                }
                            } catch (Exception e) {
                                System.out.println("Invalid file specified!");
                                e.printStackTrace();
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
                                byte[] encryptedFilename =
                                        rsaKeyHelper.encryptWithPublic(file.getBytes());
                                toServer.writeInt(encryptedFilename.length);
                                toServer.write(encryptedFilename);
                                toServer.flush();
                                packetCount += 3;
                                int fileExists = fromServer.readInt();
                                if (fileExists == PacketTypes.UPLOAD_FILE_PACKET.getValue()) {
                                    Utils.receiveEncryptedFile(fromServer, privateClientKey, "CP1",
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
                                byte[] encryptedFilename =
                                        rsaKeyHelper.encryptWithPublic(file.getBytes());
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
                            toServer.writeInt(PacketTypes.LIST_DIRECTORY_PACKET.getValue());
                            packetCount++;
                            // Receive the number of files in the directory
                            int loopTimes = fromServer.readInt();
                            while (loopTimes > 0) {
                                int numBytes = fromServer.readInt();
                                byte[] encryptedFilename = new byte[numBytes];
                                fromServer.readFully(encryptedFilename, 0, numBytes);
                                byte[] decryptedFilename =
                                        rsaKeyHelper.decryptWithPrivate(encryptedFilename);
                                System.out.println(
                                        new String(decryptedFilename, 0, decryptedFilename.length));
                                loopTimes--;
                            }
                        }
                    } else {
                        System.out.println(
                                "Invalid command received. Please try either: EXIT, UPLD <FILENAME>, DWNLD <FILENAME>, DEL <FILENAME>, or LSTDIR.");
                    }
                }
            } catch (Exception e) {
                System.out.println("Connection error! Terminating client...");
                e.printStackTrace();
            }
        }

        long timeTaken = System.nanoTime() - timeStarted;
        System.out.println("Program took: " + timeTaken / 1000000.0 + " ms to run.");
        System.out.println("Number of meaningful packets sent: " + packetCount);
        System.out.println("The method used was RSA.");
    }
}
