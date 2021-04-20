import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class AESKeyHelper {
    private SecretKey sessionKey;
    private Cipher encoder;
    private int length;

    public AESKeyHelper() {
        // TODO Auto-generated method stub
    }

    public AESKeyHelper(int length) {
        this.length = length;
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(length);
            this.sessionKey = keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void setsessionKey(SecretKey key, int length) {
        this.sessionKey = key;
        this.length = length;
    }

    public SecretKey getsessionKey() {
        return this.sessionKey;
    }

    public int getLength() {
        return this.length;
    }

    // Helper function to encode plaintext using AES according to the shared key in this class
    public byte[] encode(byte[] plaintext) {
        byte[] ciphertext = null;

        try {
            this.encoder = Cipher.getInstance("AES");
            this.encoder.init(Cipher.ENCRYPT_MODE, this.sessionKey);
            ciphertext = this.encoder.doFinal(plaintext);
        } catch (InvalidKeyException e) {
            System.out.println("ERROR: Invalid key!");
        } catch (Exception e) {
            System.out.println("ERROR: Invalid block size!");
        }

        return ciphertext;
    }

    // Helper function to decode ciphertext using AES according to the shared key in this class
    public byte[] decode(byte[] ciphertext) {
        byte[] plaintext = null;

        try {
            this.encoder = Cipher.getInstance("AES");
            this.encoder.init(Cipher.DECRYPT_MODE, this.sessionKey);
            plaintext = this.encoder.doFinal(ciphertext);
        } catch (InvalidKeyException e) {
            System.out.println("ERROR: Invalid key!");
        } catch (Exception e) {
            System.out.println("ERROR: Invalid block size!");
        }

        return plaintext;
    }
}
