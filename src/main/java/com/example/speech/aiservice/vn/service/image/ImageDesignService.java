package com.example.speech.aiservice.vn.service.image;

import com.example.speech.aiservice.vn.model.entity.chapter.Chapter;
import com.example.speech.aiservice.vn.model.entity.novel.Novel;
import com.example.speech.aiservice.vn.model.enums.FontSizeType;
import com.example.speech.aiservice.vn.service.filehandler.FileNameService;
import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;


@Service
public class ImageDesignService {

    private final FileNameService fileNameService;
    private final PropertiesService propertiesService;


    @Autowired
    public ImageDesignService(FileNameService fileNameService, PropertiesService propertiesService) {
        this.fileNameService = fileNameService;
        this.propertiesService = propertiesService;
    }

    public String installChapterFont(String imagePath, Novel novel, Chapter chapter,
                                     Map<String, int[]> totalVideoMap) {
        try {
            File inputFile = new File(imagePath);
            BufferedImage originalImage = ImageIO.read(inputFile);

            int targetWidth = originalImage.getWidth();
            int targetHeight = originalImage.getHeight(); // +150

            BufferedImage finalImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = finalImage.createGraphics();


            g2d.drawImage(originalImage, 0, 0, null); // do not shift image down

            g2d.setColor(Color.BLACK);

            // Font set "Baloo 2-Bold" for all
            FontSizeType fontSizeType = FontSizeType.SUBTEXT;
            float titleFontSize = fontSizeType.getSize();  // -> 45f

            String commonFontPath = propertiesService.getBaloo2BoldPath();
            Font commonFont = getFont(commonFontPath, titleFontSize);

            g2d.setFont(commonFont);

            float chapterFontSize = fontSizeType.getSize();  // -> 45f
            String chapterTitle = chapter.getTitle();
            g2d.setFont(getFont(commonFontPath, chapterFontSize));
            drawCenteredTextWithOutline(g2d, chapterTitle, 0, targetHeight - 15, targetWidth, Color.BLACK, Color.YELLOW);

            g2d.setFont(commonFont);

            String rangeText = null;
            for (Map.Entry<String, int[]> entry : totalVideoMap.entrySet()) {
                int[] range = entry.getValue();
                rangeText = "Chương " + range[0] + "-" + range[1];
                System.out.println(rangeText);
                break;
            }

            String extension = propertiesService.getImageExtension();
            String chapterImageDirectoryPath = propertiesService.getChapterImageDirectory();

            String safeNovelTitle = fileNameService.sanitizeFileName(novel.getTitle());
            String novelDirectory = chapterImageDirectoryPath + File.separator + safeNovelTitle;
            fileNameService.ensureDirectoryExists(novelDirectory);

            String safeChapterTitle = fileNameService.sanitizeFileName(chapter.getTitle()) + extension;
            String chapterImageFilePath = fileNameService.getAvailableFileName(novelDirectory, safeChapterTitle, extension);
            File outputFile = new File(chapterImageFilePath);
            ImageIO.write(finalImage, "png", outputFile);

            String inputImagePath = chapterImageFilePath;
            String outputImagePath = chapterImageFilePath;

            try {
                finalImage = ImageIO.read(new File(inputImagePath));
                Graphics2D g = finalImage.createGraphics();

                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                FontSizeType fontSize = FontSizeType.ULTRA_LEGEND;
                Font font = new Font("Arial", Font.BOLD, (int) fontSize.getSize());

                g.setFont(font);

                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(rangeText);
                int textHeight = fm.getAscent();
                int x = (finalImage.getWidth() - textWidth) / 2;
                int y = (finalImage.getHeight() + textHeight) / 2;

                // Very dark black border
                g.setColor(Color.BLACK);
                for (int i = 0; i < 3; i++) { // draw 3 times to make it darker
                    for (int dx = -8; dx <= 8; dx++) {
                        for (int dy = -8; dy <= 8; dy++) {
                            if (dx * dx + dy * dy <= 64) {
                                g.drawString(rangeText, x + dx, y + dy);
                            }
                        }
                    }
                }

                g.setColor(Color.YELLOW);
                g.drawString(rangeText, x, y);

                g.dispose();

                ImageIO.write(finalImage, "png", new File(outputImagePath));
                System.out.println("✅ Inserted chapter range and saved image at : " + outputImagePath);

                // String finalImagePath = addQrCodeToImage(chapterImageFilePath, "E:\\CongViecHocTap\\QR\\BIDV_QR.png"); // << Thêm QR vào

                return outputImagePath;
            } catch (IOException e) {
                System.err.println("❌ Image processing error : " + e.getMessage());
                return null;
            }


        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Failed to insert border and text for: " + chapter.getTitle());
            return null;
        }
    }

    public String installRangeChapter(String imagePath, String rangeChapter) {

        String inputImagePath = imagePath;
        String outputImagePath = inputImagePath;

        try {
            BufferedImage image = ImageIO.read(new File(inputImagePath));
            Graphics2D g = image.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);


            FontSizeType fontSize = FontSizeType.HUGE;
            Font font = new Font("Arial", Font.BOLD, (int) fontSize.getSize());
            g.setFont(font);

            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(rangeChapter);
            int textHeight = fm.getAscent();
            int x = (image.getWidth() - textWidth) / 2;
            int y = (image.getHeight() + textHeight) / 2;

            g.setColor(Color.BLACK);
            for (int i = 0; i < 3; i++) {
                for (int dx = -8; dx <= 8; dx++) {
                    for (int dy = -8; dy <= 8; dy++) {
                        if (dx * dx + dy * dy <= 64) {
                            g.drawString(rangeChapter, x + dx, y + dy);
                        }
                    }
                }
            }

            g.setColor(Color.YELLOW);
            g.drawString(rangeChapter, x, y);

            g.dispose();

            ImageIO.write(image, "png", new File(outputImagePath));
            System.out.println("✅ Inserted text into thumbnail image and saved at : " + outputImagePath);

            return outputImagePath;

        } catch (IOException e) {
            System.err.println("❌ Error : " + e.getMessage());
        }
        return null;
    }


    private Font getFont(String fontPath, float fontSize) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, new File(fontPath)).deriveFont(Font.PLAIN, fontSize);
        } catch (FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void drawCenteredTextWithOutline(Graphics2D g2d, String text, int x, int y, int boxWidth, Color borderColor, Color fontColor) {
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int drawX = x + (boxWidth - textWidth) / 2;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx != 0 || dy != 0) {
                    g2d.setColor(borderColor);
                    g2d.drawString(text, drawX + dx, y + dy);
                }
            }
        }
        g2d.setColor(fontColor);
        g2d.drawString(text, drawX, y);
    }

    public String addQrCodeToImage(String imagePath, String qrImagePath) {
        try {
            BufferedImage baseImage = ImageIO.read(new File(imagePath));
            BufferedImage qrImage = ImageIO.read(new File(qrImagePath));


            int qrWidth = (int) (qrImage.getWidth() * 0.85); // chỉ giữ 85% size gốc
            int qrHeight = (int) (qrImage.getHeight() * 0.85);


            // Resize QR
            Image resizedQrImage = qrImage.getScaledInstance(qrWidth, qrHeight, Image.SCALE_SMOOTH);
            BufferedImage qrResizedBuffered = new BufferedImage(qrWidth, qrHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gQr = qrResizedBuffered.createGraphics();
            gQr.drawImage(resizedQrImage, 0, 0, null);
            gQr.dispose();

            // Vẽ QR lên ảnh gốc
            Graphics2D g2d = baseImage.createGraphics();
            int x = 30;
            int y = baseImage.getHeight() - qrHeight - 30;
            g2d.drawImage(qrResizedBuffered, x, y, null);

            // Load font custom Baloo2-Bold
            String fontPath = "E:\\CongViecHocTap\\Font\\Baloo\\static\\Baloo2-Bold.ttf";
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File(fontPath)).deriveFont(Font.BOLD, 36f);
            g2d.setFont(customFont);

            // Ghi chữ trên QR
            String text = "Anh em ủng hộ kênh";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textX = x + (qrWidth - textWidth) / 2;
            int textY = y - 10;

            // Vẽ viền đen trước
            g2d.setColor(Color.BLACK);
            g2d.drawString(text, textX + 2, textY + 2);
            g2d.drawString(text, textX - 2, textY + 2);
            g2d.drawString(text, textX + 2, textY - 2);
            g2d.drawString(text, textX - 2, textY - 2);

            // Vẽ chữ vàng chính giữa
            g2d.setColor(Color.YELLOW);
            g2d.drawString(text, textX, textY);

            g2d.dispose();

            // Save lại ảnh
            ImageIO.write(baseImage, "png", new File(imagePath));
            return imagePath;

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to add QR code to image: " + imagePath, e);
        }
    }
}


