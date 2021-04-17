import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;

public class ServerWithSecurity {
    //include all the crt path
    String CACrt = "certificate_1004489.crt";
    String PrivateKey = "private_key.der";
    String PublicKey = "public_key.der";
    // the keys implementation for the server read are in PrivateKeyReader and the Public KeyReader class

    //from lab 5 we use the cipher class
    public static void createServerCipher(String filename) throws Exception {
        //again follow the steps from lab 5
        Cipher RsaDecipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        RsaDecipher.init(Cipher.DECRYPT_MODE,PrivateKeyReader.get(filename));
    }
    public static void createClientPublicKey(byte[] clientPublicKeyBytes){
        X509EncodedKeySpec clientKeySpecs = new X509EncodedKeySpec(clientPublicKeyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        Key clientPublicKey = kf.generatePublic(clientKeySpecs);

        System.out.println("client public key");
        System.out.println(clientPublicKey);
        Cipher clientPublicEncryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        clientPublicEncryptCipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);
    }
    
    public static void main(String[] args) {
        int port = 4321;
        if (args.length > 0)
            //get primitive dtype of certain string
            port = Integer.parseInt(args[0]);
        /*This class implements server sockets. A server socket waits for
 * requests to come in over the network. It performs some operation
 * based on that request, and then possibly returns a result to the requester.*/
        ServerSocket welcomeSocket = null;
        //client socket implementation, an endpoint for communication
        Socket connectionSocket = null;

        //file input and output for exchange
        DataOutputStream toClient = null;
        DataInputStream fromClient = null;
        //exchange files btw the buffer
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedFileOutputStream = null;

        try {
            welcomeSocket = new ServerSocket(port);
            connectionSocket = welcomeSocket.accept();
            fromClient = new DataInputStream(connectionSocket.getInputStream());
            toClient = new DataOutputStream(connectionSocket.getOutputStream());
            //int packetCount = 0;

            while (!connectionSocket.isClosed()) {
                //reads every 4 bytes interpreted as an int
                int packetType = fromClient.readInt();

                // If the packet is for transferring the filename
                if (packetType == 0) {
                    System.out.println("Receiving file...");

                    int numBytes = fromClient.readInt();
                    byte[] filename = new byte[numBytes];
                    // Must use read fully!
                    // See:
                    // https://stackoverflow.com/questions/25897627/datainputstream-read-vs-datainputstream-readfully
                    //Read fully is to read some bytes from input stream and store into buffer array number of bytes
                    //read = length of b
                    //readFully(byte b[], int off, int len)
                    /* @param      b     the buffer into which the data is read.
     * @param      off   the start offset in the data array {@code b}.
     * @param      len   the number of bytes to read.
     */
                    fromClient.readFully(filename, 0, numBytes);
                    //add code before the output stream


                    fileOutputStream =
                            new FileOutputStream("recv_" + new String(filename, 0, numBytes));

                    bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);

                    // If the packet is for transferring a chunk of the file
                } else if (packetType == 1) {
                    int numBytes = fromClient.readInt();
                    byte[] block = new byte[numBytes];
                    fromClient.readFully(block, 0, numBytes);

                    if (numBytes > 0)
                        bufferedFileOutputStream.write(block, 0, numBytes);

                    //create another if to read another integer
                    //special packetype to receive from client
                    else if(packetType == 2){
                        System.out.println("closing sockets");
                        fromClient.close();
                        toClient.close();
                        connectionSocket.close();
                    }
                    //terminating the file sequence
                    /*if (numBytes < 117) {
                        System.out.println("Closing connection...");

                        if (bufferedFileOutputStream != null)
                            bufferedFileOutputStream.close();
                        if (bufferedFileOutputStream != null)
                            fileOutputStream.close();
                        fromClient.close();
                        toClient.close();
                        connectionSocket.close();
                    }*/
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
