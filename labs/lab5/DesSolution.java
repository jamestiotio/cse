import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.crypto.*;
import java.util.Arrays;
import java.util.Base64;


public class DesSolution {
    public static void main(String[] args) throws Exception {
        System.out.println("Processing the shorttext.txt file...");
        processFile("shorttext.txt");
        System.out.println("Processing the longtext.txt file...");
        processFile("longtext.txt");
    }

    public static void processFile(String fileName) throws Exception {
        String data = "";
        String line;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
        while ((line = bufferedReader.readLine()) != null) {
            data = data + "\n" + line;
        }
        bufferedReader.close();
        System.out.println("Original content of " + fileName + ": " + data);

        // TODO: generate secret key using DES algorithm
        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
        SecretKey desKey = keyGen.generateKey();

        // TODO: create cipher object, initialize the ciphers with the given key, choose encryption
        // mode as DES
        Cipher encrypt = Cipher.getInstance("DES/ECB/PKCS5Padding");
        encrypt.init(Cipher.ENCRYPT_MODE, desKey);

        // TODO: do encryption, by calling method Cipher.doFinal().
        byte[] encryptedBytesArray = encrypt.doFinal(data.toString().getBytes());

        // TODO: print the length of output encrypted byte[], compare the length of file
        // shorttext.txt and longtext.txt
        // System.out.println("Encrypted byte array content of " + fileName + ": " +
        // Arrays.toString(encryptedBytesArray));
        System.out.println(
                "Encrypted byte array content of " + fileName + ": " + encryptedBytesArray);
        System.out.println(
                "Encrypted byte array length of " + fileName + ": " + encryptedBytesArray.length);

        // TODO: do format conversion. Turn the encrypted byte[] format into base64format String
        // using Base64
        String base64format = Base64.getEncoder().encodeToString(encryptedBytesArray);

        // TODO: print the encrypted message (in base64format String format)
        System.out.println("Ciphertext of " + fileName + ": " + base64format);
        System.out.println("Ciphertext length of " + fileName + ": " + base64format.length());

        // TODO: create cipher object, initialize the ciphers with the given key, choose decryption
        // mode as DES
        Cipher decrypt = Cipher.getInstance("DES/ECB/PKCS5Padding");
        decrypt.init(Cipher.DECRYPT_MODE, desKey);

        // TODO: do decryption, by calling method Cipher.doFinal().
        byte[] decryptedBytesArray = decrypt.doFinal(encryptedBytesArray);

        // TODO: do format conversion. Convert the decrypted byte[] to String, using "String a = new
        // String(byte_array);"
        String decryptedMessage = new String(decryptedBytesArray);

        // TODO: print the decrypted String text and compare it with original text
        System.out.println("Decrypted content of " + fileName + ": " + decryptedMessage);
    }
}
