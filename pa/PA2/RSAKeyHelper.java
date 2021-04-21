import javax.crypto.Cipher;
import java.security.*;

// Extract out common methods into this class to minimize code duplication
public class RSAKeyHelper {
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private Cipher rsaCipher;

    // Default constructor
    public RSAKeyHelper(PublicKey pubKey, PrivateKey privKey) throws Exception {
        this.publicKey = pubKey;
        this.privateKey = privKey;
        this.rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    }

    // Constructor by specifying the filepaths of private key and public key
    public RSAKeyHelper(String pubKeyFilepath, String privKeyFilepath) throws Exception {
        this.publicKey = PublicKeyReader.get(pubKeyFilepath);
        this.privateKey = PrivateKeyReader.get(privKeyFilepath);
        this.rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    }

    // Returns the private key
    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    // Returns the public key
    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    // Process plaintext by encrypting it with the public key contained in this class.
    public byte[] encryptWithPublic(byte[] plaintext) throws Exception {
        return this.encryptWithKey(plaintext, this.publicKey);
    }

    // Process plaintext by encrypting it with the private key contained in this class.
    public byte[] encryptWithPrivate(byte[] plaintext) throws Exception {
        return this.encryptWithKey(plaintext, this.privateKey);
    }

    // Process plaintext with the public/private key passed in as a parameter.
    public byte[] encryptWithKey(byte[] plaintext, Key key) throws Exception {
        this.rsaCipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] ciphertext = this.rsaCipher.doFinal(plaintext);
        return ciphertext;
    }

    // Decrypt ciphertext with the public key contained in this class.
    public byte[] decryptWithPublic(byte[] ciphertext) throws Exception {
        return this.decryptWithKey(ciphertext, this.publicKey);
    }

    // Decrypt ciphertext with the private key contained in this class.
    public byte[] decryptWithPrivate(byte[] ciphertext) throws Exception {
        return this.decryptWithKey(ciphertext, this.privateKey);
    }

    // Decrypt ciphertext with the public/private key passed in as a parameter.
    public byte[] decryptWithKey(byte[] ciphertext, Key key) throws Exception {
        this.rsaCipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plaintext = this.rsaCipher.doFinal(ciphertext);
        return plaintext;
    }
}
