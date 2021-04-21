import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.*;

// This class reads all the private key for the server code
public class PrivateKeyReader {
    public static PrivateKey get(String filename) throws Exception {
        // Extract and get private key from .der file
        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }
}
