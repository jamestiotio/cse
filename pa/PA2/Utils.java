import java.io.*;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

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
        FileInputStream fileInputStream = new FileInputStream(fileToSend);
        BufferedInputStream bufferedFileInputStream = new BufferedInputStream(fileInputStream);
        Cipher cipher = ((mode.equalsIgnoreCase("CP1")) ? Cipher.getInstance("RSA/ECB/PKCS1Padding")
                : Cipher.getInstance("AES/ECB/PKCS5Padding"));

        cipher.init(Cipher.ENCRYPT_MODE, key);

        toServer.writeInt(PacketTypes.UPLOAD_FILE_PACKET.getValue());
        // Encrypt filename
        byte[] encryptedFilename = cipher.doFinal(fileToSend.getBytes());
        // Send encrypted filename
        toServer.writeInt(encryptedFilename.length);
        toServer.write(encryptedFilename);
        toServer.flush();
        packets += 3;

        // Since we use RSA key size of 4096 bits, maximum block length is floor(4096/8) - 11 = 501
        // bytes
        byte[] buffer = ((mode.equalsIgnoreCase("CP1")) ? new byte[501] : new byte[4095]);
        int count;
        System.out.println("Sending file contents...");
        while ((count = bufferedFileInputStream.read(buffer)) > 0) {
            byte[] encryptedBuffer = cipher.doFinal(buffer);
            if (mode.equalsIgnoreCase("CP1"))
                toServer.write(encryptedBuffer, 0, 512);
            else
                toServer.write(encryptedBuffer, 0, encryptedBuffer.length);
            packets++;
            toServer.writeInt(count);
            toServer.flush();
        }

        bufferedFileInputStream.close();
        fileInputStream.close();

        return packets;
    }

    // Key can be either public, private or session key (depending on mode; assume default mode is
    // CP2)
    public static void receiveEncryptedFile(DataInputStream fromClient, Key key, String mode,
            String direction) throws Exception {
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
        String filename = direction
                + new String(decryptedFilename, 0, decryptedFilename.length).split("/")[1];

        FileOutputStream fileOutputStream = new FileOutputStream(filename);
        BufferedOutputStream bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);

        byte[] buffer = ((mode.equalsIgnoreCase("CP1")) ? new byte[512] : new byte[4096]);
        while (fromClient.read(buffer) > 0) {
            byte[] decryptedBuffer = cipher.doFinal(buffer);
            int count = fromClient.readInt();
            bufferedFileOutputStream.write(decryptedBuffer, 0, count);
            // Terminate the file sequence
            if (mode.equalsIgnoreCase("CP1")) {
                if (count < 501)
                    break;
            } else {
                if (count < 4095)
                    break;
            }
        }

        bufferedFileOutputStream.close();
        fileOutputStream.close();
        System.out.println(filename + " has been received.");
    }

    // This can be used by both sides, client and server
    // toParty is the party to verify, whereas fromParty is the party doing the verification
    // Assumes that the default version is to challenge the client and the default mode is CP2
    public static int authenticate(int nonce, DataOutputStream toParty, DataInputStream fromParty,
            String version, String mode) throws Exception {
        // Request authentication
        if (version.equalsIgnoreCase("challenge_the_server")) {
            toParty.writeInt(PacketTypes.VERIFY_SERVER_PACKET.getValue());
            toParty.flush();
        } else if (version.equalsIgnoreCase("challenge_the_client")) {
            // For some reason, putting this prevents a deadlock.
            // TODO: Fix protocol!
            Thread.sleep(1000);
            toParty.writeInt(PacketTypes.VERIFY_CLIENT_PACKET.getValue());
            toParty.flush();
        }

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
        String certificateFilename = new String(filename, 0, numBytes);
        System.out.println("Received certificate filename: " + certificateFilename);

        FileOutputStream fileOutputStream = new FileOutputStream(certificateFilename);
        BufferedOutputStream bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);

        byte[] buffer = new byte[4095];
        int count;
        while ((count = fromParty.read(buffer)) > 0) {
            bufferedFileOutputStream.write(buffer, 0, count);
            if (count < 4095) {
                break;
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
            if (mode.equalsIgnoreCase("CP1")) {
                ClientCP1.publicServerKey = publicServerKey;
            } else {
                ClientCP2.publicServerKey = publicServerKey;
            }

            // Decrypt encrypted nonce using public key
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, publicServerKey);
            byte[] decryptedNonce = cipher.doFinal(encryptedNonce);
            return ByteBuffer.wrap(decryptedNonce).getInt();
        }
        // Get back the server's nonce obtained from client
        else {
            // Reminder that client does not has a signed public key certificate in real-life
            // Thus, we only use the client's public key to verify the nonce
            // Note that we do not need to encrypt the client's public key since it is, well, public
            PublicKey publicClientKey = PublicKeyReader.get("credentials/client/public_key.der");
            if (mode.equalsIgnoreCase("CP1")) {
                ServerCP1.publicClientKey = publicClientKey;
            } else {
                ServerCP2.publicClientKey = publicClientKey;
            }

            // Decrypt encrypted nonce using public key
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, publicClientKey);
            byte[] decryptedNonce = cipher.doFinal(encryptedNonce);
            return ByteBuffer.wrap(decryptedNonce).getInt();
        }
    }

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
        int count;
        byte[] buffer = new byte[4095];
        while ((count = bufferedFileInputStream.read(buffer)) > 0) {
            toServer.write(buffer, 0, count);
            toServer.flush();
        }
        bufferedFileInputStream.close();
        fileInputStream.close();
    }

    // Can only be used by client
    public static void doClientSessionKey(DataOutputStream toServer, PublicKey key)
            throws Exception {
        AESKeyHelper aesKeyHelper = new AESKeyHelper(256);
        ClientCP2.sessionKey = aesKeyHelper.getsessionKey();
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
        ServerCP2.sessionKey = new SecretKeySpec(decryptedPublicSessionKey, 0,
                decryptedPublicSessionKey.length, "AES");
    }
}
