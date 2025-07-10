package com.example.speech.aiservice.vn.service.image;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;

@Service
public class ImageResizeService {

    public String excute(String inputImagePath, int targetWidth, int targetHeight) {
        try {
            File inputFile = new File(inputImagePath);
            BufferedImage originalImage = ImageIO.read(inputFile);

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            double aspectRatio = (double) originalWidth / originalHeight;
            System.out.printf("📐 Original image ratio: %.2f (%d x %d)%n", aspectRatio, originalWidth, originalHeight);


            // Create blurred background images
            BufferedImage finalImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = finalImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.drawImage(blurAndDarken(originalImage, targetWidth, targetHeight), 0, 0, null);

            //  Resize and center the original image
            double ratio = Math.min((double) targetWidth / originalWidth, (double) targetHeight / originalHeight);
            int newW = (int) (originalWidth * ratio);
            int newH = (int) (originalHeight * ratio);
            int x = (targetWidth - newW) / 2;
            int y = (targetHeight - newH) / 2;
            g2d.drawImage(originalImage, x, y, newW, newH, null);

            g2d.dispose();

            // Overwrite old file
            ImageIO.write(finalImage, "png", inputFile);

            System.out.println("✅ Resize successfully and overwrite at : " + inputImagePath);

            return inputImagePath;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BufferedImage blurAndDarken(BufferedImage img, int w, int h) {
        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        BufferedImage blurred = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = blurred.createGraphics();
        g2.drawImage(scaled, 0, 0, null);
        RescaleOp darken = new RescaleOp(0.6f, 0, null);
        darken.filter(blurred, blurred);
        g2.dispose();
        return blurred;
    }
}
