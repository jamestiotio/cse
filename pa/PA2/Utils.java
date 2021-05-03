import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.Key;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JTextArea;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Utils {
    // The CA's public key is visible to everyone
    private static String cacsertificate = "credentials/cacsertificate.crt";

    public static void makeFolder(String path) {
        File file = new File(path);

        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("The " + path + " directory was successfully made!");
            } else {
                System.out.println("Failed making the " + path + " directory!");
            }
        }
    }

    // Key can be either public, private or session key (depending on mode; assume default mode is
    // CP2)
    public static int sendEncryptedFile(DataOutputStream toServer, String fileToSend, Key key,
            String mode) throws Exception {
        int packets = 0;
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        Cipher cipher = ((mode.equalsIgnoreCase("CP1")) ? Cipher.getInstance("RSA/ECB/PKCS1Padding")
                : Cipher.getInstance("AES/ECB/PKCS5Padding"));
        cipher.init(Cipher.ENCRYPT_MODE, key);
        // Encrypt filename
        byte[] encryptedFilename = cipher.doFinal(fileToSend.getBytes());
        // Send encrypted filename
        toServer.writeInt(encryptedFilename.length);
        toServer.write(encryptedFilename);
        toServer.flush();
        packets += 3;

        FileInputStream fileInputStream = new FileInputStream(fileToSend);
        BufferedInputStream bufferedFileInputStream = new BufferedInputStream(fileInputStream);

        // Since we use RSA key size of 4096 bits, maximum block length is floor(4096/8) - 11 = 501
        // bytes
        int blockSize = ((mode.equalsIgnoreCase("CP1")) ? 501 : 4096);
        byte[] buffer = new byte[blockSize];
        System.out.println("Sending file contents...");
        for (boolean fileEnded = false; !fileEnded;) {
            toServer.writeInt(PacketTypes.FILE_DATA_PACKET.getValue());
            int numBytes = bufferedFileInputStream.read(buffer);
            if (numBytes == -1)
                break;
            byte[] anotherFileBuffer = Arrays.copyOfRange(buffer, 0, numBytes);
            byte[] encryptedBuffer = cipher.doFinal(anotherFileBuffer);
            fileEnded = numBytes < blockSize;
            toServer.writeInt(encryptedBuffer.length);
            toServer.writeInt(numBytes);
            toServer.write(encryptedBuffer);
            toServer.flush();
        }

        bufferedFileInputStream.close();
        fileInputStream.close();

        toServer.writeInt(PacketTypes.FILE_DIGEST_PACKET.getValue());
        // Incremental instead of loading the entire file at once to avoid OutOfMemoryError
        try (InputStream is = Files.newInputStream(Paths.get(fileToSend));
                DigestInputStream dis = new DigestInputStream(is, md)) {
            /* Read decorated stream (dis) to EOF as normal... */
        }
        byte[] digest = md.digest();
        BigInteger no = new BigInteger(1, digest);
        String hash = no.toString(16);
        while (hash.length() < 128) {
            hash = "0" + hash;
        }
        // Use the platform's default charset
        byte[] encryptedDigest = cipher.doFinal(hash.getBytes());
        toServer.writeInt(encryptedDigest.length);
        toServer.write(encryptedDigest);
        toServer.flush();
        packets += 3;

        System.out.println(fileToSend + " has been sent.");

        return packets;
    }

    // Key can be either public, private or session key (depending on mode; assume default mode is
    // CP2)
    public static void receiveEncryptedFile(DataInputStream fromClient, Key key, String mode,
            String direction) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        Cipher cipher = ((mode.equalsIgnoreCase("CP1")) ? Cipher.getInstance("RSA/ECB/PKCS1Padding")
                : Cipher.getInstance("AES/ECB/PKCS5Padding"));
        cipher.init(Cipher.DECRYPT_MODE, key);

        int numBytes = fromClient.readInt();
        byte[] encryptedFilename = new byte[numBytes];
        // Must use read fully!
        // See:
        // https://stackoverflow.com/questions/25897627/datainputstream-read-vs-datainputstream-readfully
        // Read fully is to read some bytes from input stream and store into buffer
        // array number of bytes
        // read = length of b
        // readFully(byte b[], int off, int len)
        /*
         * @param b the buffer into which the data is read.
         * 
         * @param off the start offset in the data array {@code b}.
         * 
         * @param len the number of bytes to read.
         */
        fromClient.readFully(encryptedFilename, 0, numBytes);
        byte[] decryptedFilename = cipher.doFinal(encryptedFilename);
        String[] temp = new String(decryptedFilename, 0, decryptedFilename.length).split("/");
        String currentRecursiveFolderDepth = direction;
        for (int i = 1; i < temp.length - 1; i++) {
            currentRecursiveFolderDepth = currentRecursiveFolderDepth.concat(temp[i]);
            Utils.makeFolder(currentRecursiveFolderDepth);
        }
        String filename = direction
                + new String(decryptedFilename, 0, decryptedFilename.length).split("/", 2)[1];

        FileOutputStream fileOutputStream = new FileOutputStream(filename);
        BufferedOutputStream bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);

        int blockSize = ((mode.equalsIgnoreCase("CP1")) ? 501 : 4096);
        System.out.println("Receiving file contents...");

        int packetType = fromClient.readInt();
        while (packetType == PacketTypes.FILE_DATA_PACKET.getValue()) {
            numBytes = fromClient.readInt();
            Integer end = fromClient.readInt();
            byte[] encryptedBlock = new byte[numBytes];
            fromClient.readFully(encryptedBlock, 0, numBytes);
            byte[] decryptedBlock = cipher.doFinal(encryptedBlock);

            if (end > 0)
                bufferedFileOutputStream.write(decryptedBlock, 0, decryptedBlock.length);
            if (end < blockSize) {
                if (bufferedFileOutputStream != null)
                    bufferedFileOutputStream.close();
                if (bufferedFileOutputStream != null)
                    fileOutputStream.close();
                break;
            }
            packetType = fromClient.readInt();
        }

        bufferedFileOutputStream.close();
        fileOutputStream.close();

        if (fromClient.readInt() == PacketTypes.FILE_DIGEST_PACKET.getValue()) {
            // Get expected file digest byte array
            int digestNumBytes = fromClient.readInt();
            byte[] encryptedDigest = new byte[digestNumBytes];
            fromClient.readFully(encryptedDigest, 0, digestNumBytes);
            byte[] decryptedDigest = cipher.doFinal(encryptedDigest);
            // Use the platform's default charset
            String expected = new String(decryptedDigest);

            // Get actual file digest byte array
            // Incremental instead of loading the entire file at once to avoid OutOfMemoryError
            try (InputStream is = Files.newInputStream(Paths.get(filename));
                    DigestInputStream dis = new DigestInputStream(is, md)) {
                /* Read decorated stream (dis) to EOF as normal... */
            }
            byte[] digest = md.digest();
            BigInteger no = new BigInteger(1, digest);
            String actual = no.toString(16);
            while (actual.length() < 128) {
                actual = "0" + actual;
            }

            if (actual.equals(expected)) {
                System.out.println(
                        "File digest is properly retained and integrity of file contents is verified!");
            }
        }

        System.out.println(filename + " has been received.");
    }

    // This can be used by both sides, client and server
    // toParty is the party to verify, whereas fromParty is the party doing the verification
    // Assumes that the default version is to challenge the client and the default mode is CP2
    public static int authenticate(int nonce, DataOutputStream toParty, DataInputStream fromParty,
            String version, String mode) throws Exception {
        // Send nonce
        toParty.writeInt(nonce);
        toParty.flush();
        System.out.println("Nonce sent: " + nonce);

        // Receive encrypted nonce
        int numBytes = fromParty.readInt();
        byte[] encryptedNonce = new byte[numBytes];
        fromParty.readFully(encryptedNonce, 0, numBytes);
        System.out.println("Received encrypted nonce.");

        // Receive certificate
        System.out.println("Receiving certificate...");

        // Receive filename
        numBytes = fromParty.readInt();
        byte[] filename = new byte[numBytes];
        fromParty.readFully(filename, 0, numBytes);
        String certificateFilename = "upload/" + new String(filename, 0, numBytes).split("/")[2];
        if (version.equalsIgnoreCase("challenge_the_server")) {
            certificateFilename = "download/" + new String(filename, 0, numBytes).split("/")[2];
        }
        System.out.println("Received certificate filename: " + certificateFilename);

        FileOutputStream fileOutputStream = new FileOutputStream(certificateFilename);
        BufferedOutputStream bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);

        int anotherNumBytes = fromParty.readInt();
        byte[] block = new byte[anotherNumBytes];
        fromParty.readFully(block, 0, anotherNumBytes);
        if (anotherNumBytes > 0)
            bufferedFileOutputStream.write(block, 0, anotherNumBytes);
        if (anotherNumBytes < 4095) {
            if (bufferedFileOutputStream != null) {
                bufferedFileOutputStream.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }

        bufferedFileOutputStream.close();
        fileOutputStream.close();

        System.out.println("Certificate received!");

        // Get back the client's nonce obtained from server
        if (version.equalsIgnoreCase("challenge_the_server")) {
            // Obtain public key from CA certificate
            InputStream fis = new FileInputStream(cacsertificate);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(fis);
            PublicKey keyCA = caCert.getPublicKey();

            // Verify signedCertificate with the public key from CA certificate
            InputStream fis2 = new FileInputStream(certificateFilename);
            X509Certificate signedCertificate = (X509Certificate) cf.generateCertificate(fis2);
            signedCertificate.checkValidity();
            signedCertificate.verify(keyCA);
            System.out.println("Certificate is valid and verified!");

            // Get public key from signedCertificate
            PublicKey publicServerKey = signedCertificate.getPublicKey();
            ClientWithSecurity.publicServerKey = publicServerKey;

            // Decrypt encrypted nonce using public key
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, publicServerKey);
            byte[] decryptedNonce = cipher.doFinal(encryptedNonce);

            try {
                if (fis != null)
                    fis.close();
                if (fis2 != null)
                    fis2.close();
            } catch (IOException e) {
                return ByteBuffer.wrap(decryptedNonce).getInt();
            }

            return ByteBuffer.wrap(decryptedNonce).getInt();
        }
        // Get back the server's nonce obtained from client
        else {
            // Reminder that client does not has a signed public key certificate in real-life
            // Thus, we only use the client's public key to verify the nonce
            // Note that we do not need to encrypt the client's public key since it is, well, public
            PublicKey publicClientKey = PublicKeyReader.get(certificateFilename);
            ServerWithSecurity.publicClientKey = publicClientKey;

            // Decrypt encrypted nonce using public key
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, publicClientKey);
            byte[] decryptedNonce = cipher.doFinal(encryptedNonce);
            return ByteBuffer.wrap(decryptedNonce).getInt();
        }
    }

    // Accept nonce-based challenge to prove liveness (can be used by both client and server)
    public static void acceptChallenge(DataOutputStream toClient, DataInputStream fromClient,
            PrivateKey privateKey, String fileToSend) throws Exception {
        // Receive nonce (readInt() is blocking)
        int nonce = fromClient.readInt();
        System.out.println("Nonce received: " + nonce);
        byte[] nonceBytes = ByteBuffer.allocate(4).putInt(nonce).array();

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] encryptedNonce = cipher.doFinal(nonceBytes);

        toClient.writeInt(encryptedNonce.length);
        toClient.write(encryptedNonce);
        toClient.flush();

        try {
            sendFile(toClient, fileToSend, true);
            System.out.println("Certificate sent!");
        } catch (IOException e) {
            System.out.println("Certificate not found!");
        }
    }

    public static void sendFile(DataOutputStream toServer, String fileToSend, boolean isCert)
            throws Exception {
        FileInputStream fileInputStream = new FileInputStream(fileToSend);
        BufferedInputStream bufferedFileInputStream = new BufferedInputStream(fileInputStream);
        if (!isCert)
            toServer.writeInt(PacketTypes.UPLOAD_FILE_PACKET.getValue());
        toServer.writeInt(fileToSend.getBytes().length);
        toServer.write(fileToSend.getBytes());
        toServer.flush();
        int numBytes = 0;
        byte[] buffer = new byte[4095];
        for (boolean fileEnded = false; !fileEnded;) {
            numBytes = bufferedFileInputStream.read(buffer);
            if (numBytes == -1)
                break;
            fileEnded = numBytes < 4095;

            toServer.writeInt(numBytes);
            toServer.write(buffer, 0, numBytes);
            toServer.flush();
        }
        bufferedFileInputStream.close();
        fileInputStream.close();
    }

    // Can only be used by client
    public static void doClientSessionKey(DataOutputStream toServer, PublicKey key)
            throws Exception {
        AESKeyHelper aesKeyHelper = new AESKeyHelper(256);
        ClientWithSecurity.sessionKey = aesKeyHelper.getsessionKey();
        // Encrypt session key using public key
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedSessionKey = cipher.doFinal(aesKeyHelper.getsessionKey().getEncoded());
        toServer.writeInt(encryptedSessionKey.length);
        toServer.write(encryptedSessionKey);
    }

    // Can only be used by server
    public static void doServerSessionKey(DataInputStream fromClient, PrivateKey key)
            throws Exception {
        int numBytes = fromClient.readInt();
        byte[] encryptedPublicSessionKey = new byte[numBytes];
        fromClient.readFully(encryptedPublicSessionKey, 0, numBytes);
        // Decrypt session key using private key
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedPublicSessionKey = cipher.doFinal(encryptedPublicSessionKey);
        ServerWithSecurity.sessionKey = new SecretKeySpec(decryptedPublicSessionKey, 0,
                decryptedPublicSessionKey.length, "AES");
    }

    // Modified from https://www.geeksforgeeks.org/sha-512-hash-in-java/
    public static String generateHash(String username, String password) {
        try {
            String pair = username + password;

            // getInstance() method is called with algorithm SHA-512
            MessageDigest md = MessageDigest.getInstance("SHA-512");

            // digest() method is called
            // to calculate message digest of the input string
            // returned as array of byte
            byte[] messageDigest = md.digest(pair.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

            // Add preceding/leading 0s to make it into a 128-hex digit string
            while (hashtext.length() < 128) {
                hashtext = "0" + hashtext;
            }

            // Return the HashText
            return hashtext;
        }
        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: Use JTextPane instead to allow custom text styling
    public static void addTextToScrollableTextArea(JTextArea textArea, String text) {
        textArea.append(text + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    public static void sendEncryptedPacketType(int packetType, DataOutputStream toParty) {
        // TODO
    }

    public static int receiveEncryptedPacketType(DataInputStream fromParty) {
        // TODO
        return 0;
    }

    public static void printProgressBar() {
        // TODO
    }
}
