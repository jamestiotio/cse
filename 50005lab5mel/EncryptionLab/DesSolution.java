import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.crypto.*;
import java.util.Base64;


public class DesSolution {

    public static void fileProcess(String fileName) throws Exception{
        String data = "";
        String line;
        BufferedReader bufferedReader = new BufferedReader( new FileReader(fileName));
        while((line= bufferedReader.readLine())!=null){
            data = data + "\n" + line;
        }
        System.out.println("Original content: "+ data);

//TODO: generate secret key using DES algorithm
        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
        SecretKey desKey = keyGen.generateKey();

//TODO: create cipher object, initialize the ciphers with the given key, choose encryption mode as DES
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,desKey);

//TODO: do encryption, by calling method Cipher.doFinal().
        byte[] textEncrypted = cipher.doFinal(data.getBytes());

//TODO: print the length of output encrypted byte[], compare the length of file shorttext.txt and longtext.txt

        //print content of files
        System.out.println("The content of byte array "+ fileName + ": "+ textEncrypted.toString());
        System.out.println("The byte array length of "+fileName+": "+textEncrypted.length);

//TODO: do format conversion. Turn the encrypted byte[] format into base64format String using Base64
        String base64format = Base64.getEncoder().encodeToString(textEncrypted);

//TODO: print the encrypted message (in base64format String format)
        System.out.println("The encrypted message in base64 format is :"+base64format);

//TODO: create cipher object, initialize the ciphers with the given key, choose decryption mode as DES

        //symmetric key right here so rmb to use the same key for encryption and decryption and not the different key!
        Cipher decrypt = Cipher.getInstance("DES");
        decrypt.init(Cipher.DECRYPT_MODE,desKey);

//TODO: do decryption, by calling method Cipher.doFinal().

        //java type of using decrypt to get back to original. Use that instance!
        byte[] textDecrypted = decrypt.doFinal(textEncrypted);

//TODO: do format conversion. Convert the decrypted byte[] to String, using "String a = new String(byte_array);"
        String decryptedText = new String(textDecrypted);

//TODO: print the decrypted String text and compare it with original text
        System.out.println("The decryted text is "+decryptedText);
        //System.out.println("The original text is "+data);

    }

    public static void main(String[] args) throws Exception {
        //longtext.txt
        fileProcess("longtext.txt");
        //shorttext.txt
        fileProcess("shorttext.txt");
    }
}