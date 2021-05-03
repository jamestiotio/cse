import javax.crypto.SecretKey;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;

public class ServerWithSecurity {
    // Include all the filepaths to the crt files
    private static String caCrtFilename = "credentials/server/certificate_1004555.crt";
    private static String privateKeyFilename = "credentials/server/private_key.der";
    public static PublicKey publicClientKey;
    public static SecretKey sessionKey; // Only for CP2
    // Use the SHA-512 algorithm for the stored login hashes
    private static final Set<String> storedLoginHashes = Set.of(
            "0b0a0d13f1f3d9666e9f56fbf35c11e3122809a5b3fad09f246adcfc7bc8ed2b6b8c47699189d2d8a00c40187d3571a83f7cf3c5b78066045d7cce4242971c49", // {"jamestiotio",
                                                                                                                                                // "jamestiotio"}
            "183d7d7abca514d877b85bb2596009e481ddce61a460c326b363ebed07b5ff3398de13687d3f37dfa0b54cd96a5b8ad8ad497b53bccdc69d8ddb509f5cec71e8" // {"caramelmelmel",
                                                                                                                                               // "caramelmelmel"}
    );

    public static void main(String[] args) {
        // Initial setup
        Utils.makeFolder("upload");

        // Argument parsing
        int port = 4321;
        if (args.length > 0)
            // Get primitive dtype of certain string
            port = Integer.parseInt(args[0]);

        if (args.length > 1) {
            caCrtFilename = args[1];
        }

        if (args.length == 3) {
            privateKeyFilename = args[2];
        }

        boolean shutdown = false;

        // Let the server run forever until manually killed
        while (!shutdown) {
            /*
             * This class implements server sockets. A server socket waits for requests to come in
             * over the network. It performs some operation based on that request, and then possibly
             * returns a result to the requester.
             */
            try (ServerSocket welcomeSocket = new ServerSocket(port)) {
                // Get server's private key
                PrivateKey privateServerKey = PrivateKeyReader.get(privateKeyFilename);
                // Client socket implementation, an endpoint for communication
                Socket connectionSocket = welcomeSocket.accept();
                // File input and output for exchange
                DataInputStream fromClient = new DataInputStream(connectionSocket.getInputStream());
                DataOutputStream toClient =
                        new DataOutputStream(connectionSocket.getOutputStream());
                FileOutputStream fileOutputStream = null;
                BufferedOutputStream bufferedFileOutputStream = null;

                // Flag to indicate whether current connected client is authenticated or not
                boolean authenticatedClient = false;

                String mode = "CP2"; // Server should be able to handle multiple connections of both
                                     // CP1 and CP2 at
                                     // the same time

                while (!connectionSocket.isClosed()) {
                    // Reads every 4 bytes interpreted as an int
                    int packetType = fromClient.readInt();

                    // TODO: Generate server's sequence number to prevent partial replay/playback
                    // attacks
                    int sequence = new SecureRandom().nextInt();

                    if (packetType == PacketTypes.CHANGE_MODE_PACKET.getValue()) {
                        int setMode = fromClient.readInt();
                        if (setMode == PacketTypes.CP1_MODE_PACKET.getValue()) {
                            mode = "CP1";
                        } else if (setMode == PacketTypes.CP2_MODE_PACKET.getValue()) {
                            mode = "CP2";
                        }
                        System.out.println("Mode has been set to: " + mode + ".");
                    } else if (packetType == PacketTypes.VERIFY_SERVER_PACKET.getValue()) {
                        Utils.acceptChallenge(toClient, fromClient, privateServerKey,
                                caCrtFilename);
                        toClient.flush();

                        // Generate server-side nonce to verify that server is talking to a live
                        // client
                        final int nonce = new SecureRandom().nextInt();

                        // Authenticate client and assign client's public key to variable
                        int decryptedNonce = 0;
                        System.out.println("Authenticating client...");
                        try {
                            // Request authentication
                            toClient.writeInt(PacketTypes.VERIFY_CLIENT_PACKET.getValue());
                            toClient.flush();
                            decryptedNonce = Utils.authenticate(nonce, toClient, fromClient,
                                    "challenge_the_client", mode);
                        } catch (Exception e) {
                            System.out.println("Authentication failed!");
                            e.printStackTrace();
                            if (bufferedFileOutputStream != null)
                                bufferedFileOutputStream.close();
                            if (fileOutputStream != null)
                                fileOutputStream.close();
                            fromClient.close();
                            toClient.close();
                            connectionSocket.close();
                            break;
                        }

                        if (decryptedNonce != nonce) {
                            System.out.println("Failed to authenticate client!");
                            if (bufferedFileOutputStream != null)
                                bufferedFileOutputStream.close();
                            if (fileOutputStream != null)
                                fileOutputStream.close();
                            fromClient.close();
                            toClient.close();
                            connectionSocket.close();
                            break;
                        }

                        System.out.println("Client is authenticated!");

                        // Check username and password of client
                        int authPacketType = fromClient.readInt();
                        if (authPacketType == PacketTypes.AUTH_LOGIN_USER_PACKET.getValue()) {
                            RSAKeyHelper rsaKeyHelper =
                                    new RSAKeyHelper(publicClientKey, privateServerKey);
                            int numBytes = fromClient.readInt();
                            byte[] encryptedUsername = new byte[numBytes];
                            fromClient.readFully(encryptedUsername, 0, numBytes);
                            byte[] decryptedUsername =
                                    rsaKeyHelper.decryptWithPrivate(encryptedUsername);
                            String username =
                                    new String(decryptedUsername, 0, decryptedUsername.length);
                            int anotherNumBytes = fromClient.readInt();
                            byte[] encryptedHash = new byte[anotherNumBytes];
                            fromClient.readFully(encryptedHash, 0, anotherNumBytes);
                            byte[] decryptedHash = rsaKeyHelper.decryptWithPrivate(encryptedHash);
                            String hash = new String(decryptedHash, 0, decryptedHash.length);
                            // Check if storedLoginHashes contains the specified hash
                            if (storedLoginHashes.contains(hash)) {
                                authenticatedClient = true;
                                toClient.writeInt(PacketTypes.OK_PACKET.getValue());
                                toClient.flush();
                                Utils.doServerSessionKey(fromClient, privateServerKey); // Always do
                                                                                        // this,
                                                                                        // regardless
                                                                                        // of mode!
                                System.out.println("Welcome, " + username + "!");
                            } else {
                                toClient.writeInt(PacketTypes.ERROR_PACKET.getValue());
                                toClient.flush();
                            }
                        }
                    } else if (packetType == PacketTypes.STOP_PACKET.getValue()
                            && authenticatedClient) {
                        System.out.println("Closing connection...");
                        if (bufferedFileOutputStream != null)
                            bufferedFileOutputStream.close();
                        if (fileOutputStream != null)
                            fileOutputStream.close();
                        fromClient.close();
                        toClient.close();
                        connectionSocket.close();
                        System.out.println("Connection closed properly. Bye bye!");
                        break;
                    }
                    // Client should be authenticated to be able to shut down the server!
                    else if (packetType == PacketTypes.SHUTDOWN_PACKET.getValue()
                            && authenticatedClient) {
                        shutdown = true;
                        System.out.println("Closing connection and shutting down server...");
                        if (bufferedFileOutputStream != null)
                            bufferedFileOutputStream.close();
                        if (fileOutputStream != null)
                            fileOutputStream.close();
                        fromClient.close();
                        toClient.close();
                        connectionSocket.close();
                        System.out.println("Connection closed properly. Bye bye!");
                        break;
                    } else if (packetType == PacketTypes.UPLOAD_FILE_PACKET.getValue()
                            && authenticatedClient) {
                        System.out.println("Receiving file...");
                        try {
                            long timeStarted = System.nanoTime();
                            if (mode.equalsIgnoreCase("CP1"))
                                Utils.receiveEncryptedFile(fromClient, privateServerKey, "CP1",
                                        "upload/");
                            else
                                Utils.receiveEncryptedFile(fromClient, sessionKey, "CP2",
                                        "upload/");
                            long timeTaken = System.nanoTime() - timeStarted;
                            System.out.println("Upload took: " + timeTaken / 1000000.0 + " ms");
                            String successMessage = "File received!";
                            System.out.println(successMessage);
                            toClient.writeInt(successMessage.getBytes().length);
                            toClient.write(successMessage.getBytes());
                            toClient.flush();
                        } catch (IOException e) {
                            String failureMessage = "Failed to receive file!";
                            System.out.println(failureMessage);
                            toClient.writeInt(failureMessage.getBytes().length);
                            toClient.write(failureMessage.getBytes());
                            toClient.flush();
                            e.printStackTrace();
                        }
                    } else if (packetType == PacketTypes.DOWNLOAD_FILE_PACKET.getValue()
                            && authenticatedClient) {
                        int numBytes = fromClient.readInt();
                        byte[] encryptedFilename = new byte[numBytes];
                        fromClient.readFully(encryptedFilename, 0, numBytes);
                        RSAKeyHelper rsaKeyHelper =
                                new RSAKeyHelper(publicClientKey, privateServerKey);
                        AESCipherHelper aesCipherHelper = new AESCipherHelper(sessionKey);
                        byte[] decryptedFilename = ((mode.equalsIgnoreCase("CP1"))
                                ? rsaKeyHelper.decryptWithPrivate(encryptedFilename)
                                : aesCipherHelper.decryptWithKey(encryptedFilename));
                        String fileToSend = "upload/"
                                + new String(decryptedFilename, 0, decryptedFilename.length);
                        System.out.println("Sending file...");
                        try {
                            toClient.writeInt(PacketTypes.UPLOAD_FILE_PACKET.getValue());
                            if (mode.equalsIgnoreCase("CP1"))
                                Utils.sendEncryptedFile(toClient, fileToSend, publicClientKey,
                                        "CP1");
                            else
                                Utils.sendEncryptedFile(toClient, fileToSend, sessionKey, "CP2");
                        } catch (Exception e) {
                            toClient.writeInt(PacketTypes.ERROR_PACKET.getValue());
                        }
                    } else if (packetType == PacketTypes.DELETE_FILE_PACKET.getValue()
                            && authenticatedClient) {
                        int numBytes = fromClient.readInt();
                        byte[] encryptedFilename = new byte[numBytes];
                        fromClient.readFully(encryptedFilename, 0, numBytes);
                        RSAKeyHelper rsaKeyHelper =
                                new RSAKeyHelper(publicClientKey, privateServerKey);
                        AESCipherHelper aesCipherHelper = new AESCipherHelper(sessionKey);
                        byte[] decryptedFilename = ((mode.equalsIgnoreCase("CP1"))
                                ? rsaKeyHelper.decryptWithPrivate(encryptedFilename)
                                : aesCipherHelper.decryptWithKey(encryptedFilename));
                        File file = new File("upload/"
                                + new String(decryptedFilename, 0, decryptedFilename.length));
                        if (file.delete()) {
                            String successMessage = "File is successfully deleted!";
                            System.out.println(successMessage);
                            toClient.writeInt(successMessage.getBytes().length);
                            toClient.write(successMessage.getBytes());
                            toClient.flush();
                        } else {
                            String failureMessage = "File deletion failed!";
                            System.out.println(failureMessage);
                            toClient.writeInt(failureMessage.getBytes().length);
                            toClient.write(failureMessage.getBytes());
                            toClient.flush();
                        }
                    } else if (packetType == PacketTypes.LIST_DIRECTORY_PACKET.getValue()
                            && authenticatedClient) {
                        File directory = new File("upload");
                        String[] files = directory.list();
                        toClient.writeInt(files.length);
                        RSAKeyHelper rsaKeyHelper =
                                new RSAKeyHelper(publicClientKey, privateServerKey);
                        AESCipherHelper aesCipherHelper = new AESCipherHelper(sessionKey);
                        for (String filename : files) {
                            byte[] encryptedFilename = ((mode.equalsIgnoreCase("CP1"))
                                    ? rsaKeyHelper.encryptWithPublic(filename.getBytes())
                                    : aesCipherHelper.encryptWithKey(filename.getBytes()));
                            toClient.writeInt(encryptedFilename.length);
                            toClient.write(encryptedFilename, 0, encryptedFilename.length);
                            toClient.flush();
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof EOFException) {
                    System.out.println("Connection not closed properly. Bye bye!");
                } else {
                    e.printStackTrace();
                }
            }
        }
    }
}
