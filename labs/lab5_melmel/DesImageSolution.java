import java.lang.Object;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.image.BufferedImage;
import java.nio.*;
import javax.crypto.*;
import java.util.Base64;


public class DesImageSolution {
    public static int convertByteArrayToInt(byte[] data) {
        if (data == null || data.length != 4) return 0x0;
        // ----------
        return (int)( // NOTE: type cast not necessary for int
                (0xff & data[0]) << 24  |
                        (0xff & data[1]) << 16  |
                        (0xff & data[2]) << 8   |
                        (0xff & data[3]) << 0
        );
    }

    public static int[] convertByteArrayToIntArray(byte[] data) {
        if (data == null || data.length % 4 != 0) return null;
        // ----------
        int[] ints = new int[data.length / 4];
        for (int i = 0; i < ints.length; i++)
            ints[i] = ( convertByteArrayToInt(new byte[] {
                    data[(i*4)],
                    data[(i*4)+1],
                    data[(i*4)+2],
                    data[(i*4)+3],
            } ));
        return ints;
    }
    public static void main(String[] args) throws Exception{
        int image_width = 200;
        int image_length = 200;

        // read image file and save pixel value into int[][] imageArray
        //BufferedImage img = ImageIO.read(new File("SUTD.bmp"));
        BufferedImage img = ImageIO.read(new File("triangle.bmp"));
        image_width = img.getWidth();
        image_length = img.getHeight();

        // byte[][] imageArray = new byte[image_width][image_length];
        int[][] imageArray = new int[image_width][image_length];
        for(int idx = 0; idx < image_width; idx++) {
            for(int idy = 0; idy < image_length; idy++) {
                int color = img.getRGB(idx, idy);
                imageArray[idx][idy] = color;            
            }
        } 
// TODO: generate secret key using DES algorithm
        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
        SecretKey secKey = keyGen.generateKey();

// TODO: Create cipher object, initialize the ciphers with the given key, choose encryption algorithm/mode/padding,
//you need to try both ECB and CBC mode, use PKCS5Padding padding method
        //modify the above method to do so change the method latere accordingly
        //Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,secKey);

        // define output BufferedImage, set size and format
        BufferedImage outImage = new BufferedImage(image_width,image_length, BufferedImage.TYPE_3BYTE_BGR);

        for(int idx = 0; idx < image_width; idx++) {
        // convert each column int[] into a byte[] (each_width_pixel)
            byte[] each_width_pixel = new byte[4*image_length];
            //for(int idy = 0; idy < image_length; idy++)
            for (int idy = image_length - 1; idy > 0; idy--){
                ByteBuffer dbuf = ByteBuffer.allocate(4);
                //for reverse
                //dbuf.putInt(imageArray[idx][image_length-idy]);
                // for the normal
                dbuf.putInt(imageArray[idx][idy]);
                byte[] bytes = dbuf.array();
                System.arraycopy(bytes, 0, each_width_pixel, idy*4, 4);
            }

// TODO: encrypt each column or row bytes
            byte[] colEncrypted = cipher.doFinal(each_width_pixel);

// TODO: convert the encrypted byte[] back into int[] and write to outImage (use setRGB)
            int[] convertedByteArray = convertByteArrayToIntArray(colEncrypted);
            for(int idy = 0; idy < image_length; idy++) {
                outImage.setRGB(idx, idy, convertedByteArray[idy]);
            }

        }
//write outImage into file
        //ImageIO.write(outImage, "BMP",new File("CBC.bmp"));
        ImageIO.write(outImage, "BMP",new File("triangle_new.bmp"));
    }
}