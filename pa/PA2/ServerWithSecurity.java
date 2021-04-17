import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerWithSecurity {
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

                    if (numBytes < 117) {
                        System.out.println("Closing connection...");

                        if (bufferedFileOutputStream != null)
                            bufferedFileOutputStream.close();
                        if (bufferedFileOutputStream != null)
                            fileOutputStream.close();
                        fromClient.close();
                        toClient.close();
                        connectionSocket.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
