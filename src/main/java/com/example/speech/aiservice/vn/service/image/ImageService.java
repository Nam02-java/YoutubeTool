package com.example.speech.aiservice.vn.service.image;

import com.example.speech.aiservice.vn.service.filehandler.FileNameService;
import com.example.speech.aiservice.vn.service.google.GoogleChromeLauncherService;
import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import com.example.speech.aiservice.vn.service.google.WebDriverLauncherService;
import com.example.speech.aiservice.vn.service.wait.WaitService;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.net.ssl.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Service
public class ImageService {

    private final PropertiesService propertiesService;
    private final FileNameService fileNameService;
    private final WaitService waitService;
    private final ImageSharpenService imageSharpenService;
    private final ImageResizeService imageResizeService;

    @Autowired
    public ImageService(PropertiesService propertiesService, FileNameService fileNameService, WaitService waitService, ImageSharpenService imageSharpenService, ImageResizeService imageResizeService) {
        this.propertiesService = propertiesService;
        this.fileNameService = fileNameService;
        this.waitService = waitService;
        this.imageSharpenService = imageSharpenService;
        this.imageResizeService = imageResizeService;
    }

    public String getValidImagePath(WebDriver driver, Process chromeProcess, String inputLink, GoogleChromeLauncherService googleChromeLauncherService, WebDriverLauncherService webDriverLauncherService,
                                    String port, String seleniumFile,
                                    String safeTitle) throws IOException {

        int maxRetries = 10;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                String result = tryGetValidImagePath(driver, safeTitle);
                if (result != null) {
                    return result;
                }
            } catch (TimeoutException e) {
                System.out.println("⚠️ Timeout when waiting for image to load. Retrying... (" + (attempt + 1) + ")");
                driver.navigate().refresh();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            } finally {
                googleChromeLauncherService.shutdown(chromeProcess);
                webDriverLauncherService.shutDown(driver);
            }
            attempt++;
            waitService.waitForSeconds(5);

            chromeProcess = googleChromeLauncherService.openGoogleChrome(port, seleniumFile);
            driver = webDriverLauncherService.initWebDriver(port);
            waitService.waitForSeconds(5);
            driver.get(inputLink);
            waitService.waitForSeconds(5);
        }
        throw new RuntimeException("❌ Failed to get valid image after " + maxRetries + " attempts.");
    }

    private String tryGetValidImagePath(WebDriver driver, String safeTitle) throws TimeoutException {
        String imageDirectoryPath = propertiesService.getImageDirectory();

        String safeNovelTitle = fileNameService.sanitizeFileName(safeTitle);
        String imageDirectory = imageDirectoryPath + File.separator + safeNovelTitle;
        fileNameService.ensureDirectoryExists(imageDirectory);

        String baseFileName = safeTitle;
        String extension = propertiesService.getImageExtension();

        File dir = new File(imageDirectory);
        String firstPngPath = null;

        if (dir.exists() && dir.isDirectory()) {
            File[] pngFiles = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".png");
                }
            });

            if (pngFiles != null && pngFiles.length > 0) {
                firstPngPath = pngFiles[0].getAbsolutePath();
                System.out.println("✅ First image in file : " + firstPngPath);
                return firstPngPath.toString();
            }
        }


        String imageFilePath = fileNameService.getAvailableFileNameWithNoNumber(imageDirectory, baseFileName, extension);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        By imgSelector = By.cssSelector("body > main > div:nth-child(1) > div.novel > div.n-img > img");

        System.out.println("⏳ Waiting for image element...");
        WebElement img = wait.until(ExpectedConditions.presenceOfElementLocated(imgSelector));
        System.out.println("✅ Image element found. Scrolling into view...");

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", img
        );

        boolean loaded = false;
        for (int retry = 0; retry < 3; retry++) {
            try {
                loaded = wait.until(driver1 -> {
                    try {
                        WebElement imageElement = driver1.findElement(imgSelector);
                        return (Boolean) ((JavascriptExecutor) driver1).executeScript(
                                "return arguments[0].complete && arguments[0].naturalWidth > 100;",
                                imageElement
                        );
                    } catch (Exception e) {
                        return false;
                    }
                });
                if (loaded) break;
            } catch (TimeoutException e) {
                System.out.println("⚠️ Retry image load failed (" + (retry + 1) + ")");
                waitService.waitForSeconds(2);
            }
        }

        if (!loaded) {
            throw new TimeoutException("❌ Image did not load after retries.");
        }

        String imgUrl = img.getAttribute("src");
        System.out.println("📷 Image src: " + imgUrl);

        try {
            File screenshot = img.getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), Paths.get(imageFilePath));
            System.out.println("✅ Saved cover image using screenshot: " + imageFilePath);
        } catch (Exception ex) {
            System.out.println("⚠️ Screenshot failed. Trying to download from URL...");
            try {
                BufferedImage image = readImageFromUrl(imgUrl);
                ImageIO.write(image, "jpg", new File(imageFilePath));
                System.out.println("✅ Cover photo downloaded and saved in: " + imageFilePath);
            } catch (IOException e) {
                throw new RuntimeException("❌ Failed to download image from: " + imgUrl, e);
            }
        }


        String enhanceAndResizeImage = enhanceAndResizeImage(imageFilePath); //imageFilePath

//        String finalImagePath = addQrCodeToImage(enhanceAndResizeImage, "E:\\CongViecHocTap\\QR\\BIDV_QR.png"); // << Thêm QR vào
        return enhanceAndResizeImage;
    }

    private String enhanceAndResizeImage(String inputPath) {
        String sharpenedPath = inputPath;

        // YouTube standard size
        int targetWidth = 1920;
        int targetHeight = 1080;
        sharpenedPath = imageSharpenService.excute(sharpenedPath);
        String finalPath = imageResizeService.excute(sharpenedPath, targetWidth, targetHeight);
        return finalPath;
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
            String fontPath = propertiesService.getBaloo2BoldPath();
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


    private BufferedImage readImageFromUrl(String imageUrl) throws IOException {
        disableSSLVerification();

        URL url = new URL(imageUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        conn.setRequestProperty("Referer", "https://ixdzs8.com/");
        conn.setRequestProperty("Accept", "image/*,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        conn.setInstanceFollowRedirects(true);
        conn.connect();

        int responseCode = conn.getResponseCode();
        System.out.println("Image URL Response Code: " + responseCode);

        if (responseCode != 200) {
            throw new IOException("❌ Server returned non-OK status: " + responseCode);
        }

        try (InputStream in = conn.getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IOException("❌ ImageIO.read() returned null. Unsupported image format or empty stream.");
            }
            return image;
        }
    }

    private static void disableSSLVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}