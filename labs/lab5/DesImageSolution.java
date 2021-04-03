import java.lang.Object;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.image.BufferedImage;
import java.nio.*;
import javax.crypto.*;
import java.util.Base64;


public class DesImageSolution {
    private static final String file1 = "SUTD.bmp";
    private static final String file2 = "triangle.bmp";

    public static void main(String[] args) throws Exception {
        System.out.println("Doing ECB...");
        processImage(file1, "ECB", "regular");
        System.out.println("\nDoing CBC...");
        processImage(file1, "CBC", "regular");
        System.out.println("\nDoing CBC reversed...");
        processImage(file2, "CBC", "reversed");
    }

    public static void processImage(String fileName, String mode, String option) throws Exception {
        // read image file and save pixel value into int[][] imageArray
        BufferedImage img = ImageIO.read(new File(fileName));
        int image_width = img.getWidth();
        int image_length = img.getHeight();
        // byte[][] imageArray = new byte[image_width][image_length];
        int[][] imageArray = new int[image_width][image_length];
        for (int idx = 0; idx < image_width; idx++) {
            for (int idy = 0; idy < image_length; idy++) {
                int color = img.getRGB(idx, idy);
                imageArray[idx][idy] = color;
            }
        }

        // TODO: generate secret key using DES algorithm
        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
        SecretKey desKey = keyGen.generateKey();

        // TODO: create cipher object, initialize the ciphers with the given key, choose encryption
        // algorithm/mode/padding, you need to try both ECB and CBC mode, use PKCS5Padding padding
        // method
        Cipher encrypt = Cipher.getInstance("DES/" + mode + "/PKCS5Padding");
        encrypt.init(Cipher.ENCRYPT_MODE, desKey);

        // define output BufferedImage, set size and format
        BufferedImage outImage =
                new BufferedImage(image_width, image_length, BufferedImage.TYPE_3BYTE_BGR);

        for (int idx = 0; idx < image_width; idx++) {
            // convert each column int[] into a byte[] (each_width_pixel)
            byte[] each_width_pixel = new byte[4 * image_length];
            if (option.equalsIgnoreCase("reversed")) {
                for (int idy = image_length - 1; idy > 0; idy--) {
                    ByteBuffer dbuf = ByteBuffer.allocate(4);
                    dbuf.putInt(imageArray[idx][image_length - idy]);
                    byte[] bytes = dbuf.array();
                    System.arraycopy(bytes, 0, each_width_pixel, idy * 4, 4);
                }
            } else {
                for (int idy = 0; idy < image_length; idy++) {
                    ByteBuffer dbuf = ByteBuffer.allocate(4);
                    dbuf.putInt(imageArray[idx][idy]);
                    byte[] bytes = dbuf.array();
                    System.arraycopy(bytes, 0, each_width_pixel, idy * 4, 4);
                }
            }

            // TODO: encrypt each column or row bytes
            byte[] encrypted = encrypt.doFinal(each_width_pixel);

            // TODO: convert the encrypted byte[] back into int[] and write to outImage (use setRGB)
            byte[] pixel = new byte[4];
            for (int idy = 0; idy < image_length; idy++) {
                System.arraycopy(encrypted, 4 * idy, pixel, 0, 4);
                ByteBuffer buf = ByteBuffer.wrap(pixel);
                int rgb = buf.getInt();
                outImage.setRGB(idx, idy, rgb);
            }
        }

        // write outImage into file
        if (fileName == "SUTD.bmp") {
            ImageIO.write(outImage, "BMP", new File(mode.toLowerCase() + ".bmp"));
        } else if (fileName == "triangle.bmp") {
            ImageIO.write(outImage, "BMP", new File("triangle_new.bmp"));
        } else {
            String newFileName = fileName;
            int lastFullStopIndex = fileName.lastIndexOf(".");
            if (lastFullStopIndex != -1)
                newFileName = fileName.substring(0, lastFullStopIndex);
            ImageIO.write(outImage, "BMP",
                    new File(newFileName + "_" + mode + "_" + option + "_encrypted.bmp"));
        }
    }
}
