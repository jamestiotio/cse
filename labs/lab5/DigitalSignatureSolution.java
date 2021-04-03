import java.util.Base64;
import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.*;


public class DigitalSignatureSolution {
    public static void main(String[] args) throws Exception {
        processFile("shorttext.txt");
        processFile("longtext.txt");
    }

    public static void processFile(String fileName) throws Exception {
        // Read the text file and save to String data
        String data = "";
        String line;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
        while ((line = bufferedReader.readLine()) != null) {
            data = data + "\n" + line;
        }
        bufferedReader.close();
        System.out.println("Original content: " + data);

        // TODO: generate a RSA keypair, initialize as 1024 bits, get public key and private key
        // from this keypair.
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.generateKeyPair();
        Key publicKey = keyPair.getPublic();
        Key privateKey = keyPair.getPrivate();

        // TODO: Calculate message digest, using MD5 hash function
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data.toString().getBytes());
        byte[] digest = md.digest();

        // TODO: print the length of output digest byte[], compare the length of file shorttext.txt
        // and longtext.txt
        System.out.println(
                "MD5 digest of " + fileName + ": " + Base64.getEncoder().encodeToString(digest));
        System.out.println("MD5 digest length of " + fileName + ": " + digest.length);

        // TODO: Create RSA("RSA/ECB/PKCS1Padding") cipher object and initialize is as encrypt mode,
        // use PRIVATE key.
        Cipher encrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        encrypt.init(Cipher.ENCRYPT_MODE, privateKey);

        // TODO: encrypt digest message
        byte[] encrypted = encrypt.doFinal(digest);

        // TODO: print the encrypted message (in base64format String using Base64)
        System.out.println("Encrypted digest of " + fileName + ": "
                + Base64.getEncoder().encodeToString(encrypted));
        System.out.println("Signed digest length of " + fileName + ": " + encrypted.length);

        // TODO: Create RSA("RSA/ECB/PKCS1Padding") cipher object and initialize is as decrypt mode,
        // use PUBLIC key.
        Cipher decrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        decrypt.init(Cipher.DECRYPT_MODE, publicKey);

        // TODO: decrypt message
        byte[] decrypted = decrypt.doFinal(encrypted);

        // TODO: print the decrypted message (in base64format String using Base64), compare with
        // origin digest
        System.out.println("Decrypted digest of " + fileName + ": "
                + Base64.getEncoder().encodeToString(decrypted));
    }
}
