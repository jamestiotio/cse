import java.util.Base64;
import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.*;


public class DigitalSignatureSolution {

    public static void main(String[] args) throws Exception {
//Read the text file and save to String data
            String fileName = "shorttext.txt";
            String data = "";
            String line;
            BufferedReader bufferedReader = new BufferedReader( new FileReader(fileName));
            while((line= bufferedReader.readLine())!=null){
                data = data +"\n" + line;
            }
            System.out.println("Original content: "+ data);

//TODO: generate a RSA keypair, initialize as 1024 bits, get public key and private key from this keypair.


//TODO: Calculate message digest, using MD5 hash function


//TODO: print the length of output digest byte[], compare the length of file shorttext.txt and longtext.txt

           
//TODO: Create RSA("RSA/ECB/PKCS1Padding") cipher object and initialize is as encrypt mode, use PRIVATE key.


//TODO: encrypt digest message


//TODO: print the encrypted message (in base64format String using Base64) 

//TODO: Create RSA("RSA/ECB/PKCS1Padding") cipher object and initialize is as decrypt mode, use PUBLIC key.           

//TODO: decrypt message

//TODO: print the decrypted message (in base64format String using Base64), compare with origin digest 



    }

}