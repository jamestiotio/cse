import javax.crypto.Cipher;
import javax.crypto.SecretKey;

// Extract out common methods into this class to minimize code duplication
public class AESCipherHelper {
    private SecretKey sessionKey;
    private Cipher aesCipher;

    // Default constructor
    public AESCipherHelper(SecretKey sessionKey) throws Exception {
        this.sessionKey = sessionKey;
        this.aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    }

    // Returns the session key
    public SecretKey getSessionKey() {
        return this.sessionKey;
    }

    // Process plaintext by encrypting it with the session key contained in this class.
    public byte[] encryptWithKey(byte[] plaintext) throws Exception {
        this.aesCipher.init(Cipher.ENCRYPT_MODE, this.sessionKey);
        byte[] ciphertext = this.aesCipher.doFinal(plaintext);
        return ciphertext;
    }

    // Decrypt ciphertext with the session key contained in this class.
    public byte[] decryptWithKey(byte[] ciphertext) throws Exception {
        this.aesCipher.init(Cipher.DECRYPT_MODE, this.sessionKey);
        byte[] plaintext = this.aesCipher.doFinal(ciphertext);
        return plaintext;
    }
}
