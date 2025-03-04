package com.llm.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class ImageUtil {

    /**
     * Converts a base64-encoded image string to a BufferedImage.
     *
     * @param base64Image the base64-encoded image string
     * @return the decoded BufferedImage, or null if decoding fails
     */
    public static BufferedImage decodeBase64ToImage(String base64Image) {
        try {
            // Decode the base64 string
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            // Convert the byte array to a BufferedImage
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(bis);
            bis.close();

            return image;
        } catch (IOException e) {
            System.err.println("Error while decoding base64 image string: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves a BufferedImage to a file.
     *
     * @param image the BufferedImage to save
     * @param format the format of the image (e.g., "png" or "jpg")
     * @param filePath the path where the image should be saved
     * @return true if the image was saved successfully, false otherwise
     */
    public static boolean saveImageToFile(BufferedImage image, String format, String filePath) {
        try {
            File outputFile = new File(filePath);
            ImageIO.write(image, format, outputFile);
            return true;
        } catch (IOException e) {
            System.err.println("Error saving image to file: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        // Example base64 image string (replace with actual base64 string)
        String base64Image = "your_base64_image_string_here";

        // Decode base64 to image
        BufferedImage image = decodeBase64ToImage(base64Image);

        if (image != null) {
            // Write image to file
            String filePath = "output_image.png"; // specify the desired file path and format
            boolean success = saveImageToFile(image, "png", filePath);

            if (success) {
                System.out.println("Image successfully parsed and saved as '" + filePath + "'!");
            } else {
                System.out.println("Failed to save the image.");
            }
        } else {
            System.out.println("Failed to decode base64 image.");
        }
    }
}
