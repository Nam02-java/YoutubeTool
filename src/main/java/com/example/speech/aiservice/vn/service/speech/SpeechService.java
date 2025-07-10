package com.example.speech.aiservice.vn.service.speech;

import com.example.speech.aiservice.vn.dto.response.TextToSpeechResponseDTO;
import com.example.speech.aiservice.vn.model.entity.chapter.Chapter;
import com.example.speech.aiservice.vn.model.entity.novel.Novel;
import com.example.speech.aiservice.vn.service.audio.AudioMergerService;
import com.example.speech.aiservice.vn.service.filehandler.FileNameService;
import com.example.speech.aiservice.vn.service.filehandler.FileReaderService;
import com.example.speech.aiservice.vn.service.google.GoogleAudioDownloaderService;
import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import com.example.speech.aiservice.vn.service.wait.WaitService;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.MovieBox;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SpeechService {
    private final FileNameService fileNameService;
    private final FileReaderService fileReaderService;
    private final WaitService waitService;
    private final GoogleAudioDownloaderService googleAudioDownloaderService;
    private final PropertiesService propertiesService;
    private final AudioMergerService audioMergerService;

    @Autowired
    public SpeechService(
            FileNameService fileNameService,
            FileReaderService fileReaderService,
            WaitService waitService,
            GoogleAudioDownloaderService googleAudioDownloaderService,
            PropertiesService propertiesService,
            AudioMergerService audioMergerService
    ) {
        this.fileNameService = fileNameService;
        this.fileReaderService = fileReaderService;
        this.waitService = waitService;
        this.googleAudioDownloaderService = googleAudioDownloaderService;
        this.propertiesService = propertiesService;
        this.audioMergerService = audioMergerService;
    }

    public TextToSpeechResponseDTO textToSpeechResponseDTO(WebDriver driver, String contentFilePath, Novel novel, Chapter chapter) throws IOException {

        final int MAX_RETRIES = 10;
        int attempt = 0;

        List<String> inputFilePaths = null;

        String voiceDirectoryPath = null;
        String voiceFileExtension = null;
        String textToSpeechUrl = null;

        String audioUrl = null;
        String audioFilePath = null;
        String novelDirectory = null;
        String safeChapterTitle = null;
        String contentValue = null;

        WebDriverWait wait = null;

        while (attempt < MAX_RETRIES) {

            attempt++;
            System.out.printf("🌀 Chapter %d – Attempt %d of %d%n", chapter.getChapterNumber(), attempt, MAX_RETRIES);

            try {
                inputFilePaths = new ArrayList<>();

                voiceDirectoryPath = propertiesService.getVoiceDirectory();
                voiceFileExtension = propertiesService.getVoiceFileExtension();
                textToSpeechUrl = propertiesService.getTextToSpeechUrl();

                driver.get(textToSpeechUrl);

                waitService.waitForSeconds(10);

                driver.get(textToSpeechUrl);


                String content = fileReaderService.readFileContent(contentFilePath);
                List<String> chunks = splitContentIntoChunks(content);
                System.out.println(String.format("📖 Chapter %d - 📦 Chunks: %d", chapter.getChapterNumber(), chunks.size()));

                boolean maxPingDeadNumber = false;

                for (String chunk : chunks) {
                    driver.get(textToSpeechUrl);


                    waitService.waitForSeconds(5); // 2
                    WebElement textArea = driver.findElement(By.cssSelector("#edit-content"));

                    waitService.waitForSeconds(2);
                    textArea.clear();

                    waitService.waitForSeconds(5);
                    textArea.sendKeys(chunk);

                  waitService.waitForSeconds(2); // default is 2

                    // Wait until the text area value is not empty or until timeout (e.g., 10000000s)
                    wait = new WebDriverWait(driver, Duration.ofSeconds(10000000));
                    wait.until(driver1 -> {
                        String value = driver1.findElement(By.cssSelector("#edit-content")).getAttribute("value");
                        return value != null && !value.trim().isEmpty();
                    });


                    if (textArea.isDisplayed() && textArea.isEnabled()) {
                        contentValue = textArea.getAttribute("value");
                        if (contentValue == null || contentValue.trim().isEmpty()) {
                            System.out.println("TextArea is empty.");
                        } else {
                            System.out.println("📝 Chapter " + chapter.getChapterNumber() + " - Text area content: \"" + contentValue + "\"");
                        }
                    } else {
                        System.out.println("TextArea is not available.");
                    }


                    //Disable ADS
                    ((JavascriptExecutor) driver).executeScript("document.getElementById('waves').style.display='none';");
                    waitService.waitForSeconds(1);
                    wait = new WebDriverWait(driver, Duration.ofSeconds(5)); // Default : 10
                    WebElement submitButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("submit_btn")));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitButton);
                    waitService.waitForSeconds(1);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);

                    waitService.waitForSeconds(2);

                    final int MAX_RETRIES_DEAD_NUMBER = 10;
                    int attemptDeadNumber = 0;
                    while (attemptDeadNumber < MAX_RETRIES_DEAD_NUMBER) {
                        attemptDeadNumber++;
                        System.out.printf("🌀 Chapter %d – Attempt %d of %d to kill dead number!%n",
                                chapter.getChapterNumber(), attemptDeadNumber, MAX_RETRIES_DEAD_NUMBER);

                        driver.findElement(By.id("submit_btn")).click();

                        waitService.waitForSeconds(5); //5

                        wait = new WebDriverWait(driver, Duration.ofSeconds(10000000));
                        wait.until(driver1 -> {
                            JavascriptExecutor js = (JavascriptExecutor) driver1;
                            String display = (String) js.executeScript(
                                    "return window.getComputedStyle(document.getElementById('btn_text')).getPropertyValue('display');"
                            );
                            return display != null && !display.equals("none");
                        });

                        waitService.waitForSeconds(5);

                        JavascriptExecutor js = (JavascriptExecutor) driver;
                        Object duration = js.executeScript(
                                "const audio = document.getElementById('audio'); return (audio && !isNaN(audio.duration)) ? audio.duration : null;"
                        );

                        System.out.println("🌐 Chapter " + chapter.getChapterNumber() + " - Audio duration on website: " + duration + " seconds");

                        double durationNumber = 0.0; // Giá trị mặc định
                        if (duration instanceof Number) {
                            durationNumber = ((Number) duration).doubleValue();
                        } else {
                            System.out.println("⚠️ Duration is null or invalid, using default value 0.0");
                        }

                        double deadNumber = 4.572018;

                        if (Math.abs(durationNumber - deadNumber) < 0.0001) {
                            System.out.println("💩 Chapter " + chapter.getChapterNumber() + " - ✅\uD83D\uDC80 Exact match duration (dead number) " + deadNumber + " seconds");
                            System.out.println("💩📝 Chapter " + chapter.getChapterNumber() + " - Text area content: \"" + contentValue + "\"");

                            waitService.waitForSeconds(2);
                            driver.get(textToSpeechUrl);

                            waitService.waitForSeconds(5); // 2
                             textArea = driver.findElement(By.cssSelector("#edit-content"));

                            waitService.waitForSeconds(2);
                            textArea.clear();

                            waitService.waitForSeconds(5);
                            textArea.sendKeys(chunk);
                            waitService.waitForSeconds(20);

                            continue;
                        }
                        break;
                    }

                    if (attemptDeadNumber == MAX_RETRIES_DEAD_NUMBER) {
                        maxPingDeadNumber = true;
                        break;
                    }


                    while (true) {
                        audioUrl = driver.findElement(By.id("audio")).getAttribute("src");
                        if (audioUrl != null && !audioUrl.trim().isEmpty()) {
                            System.out.println("\uD83C\uDF10 Chapter " + chapter.getChapterNumber() + " - 🎧 Audio URL: [" + audioUrl + "]");
                            break;
                        }

                        throw new RuntimeException(String.format(
                                "🔥💩 Chapter %d 💀 Failed to get audio URL after %d attempts – forcing site reset!",
                                chapter.getChapterNumber(), attempt
                        ));
                    }

                    String safeNovelTitle = fileNameService.sanitizeFileName(novel.getTitle());
                    novelDirectory = voiceDirectoryPath + File.separator + safeNovelTitle;
                    fileNameService.ensureDirectoryExists(novelDirectory);

                    safeChapterTitle = fileNameService.sanitizeFileName(chapter.getTitle());
                    audioFilePath = fileNameService.getAvailableFileName(novelDirectory, safeChapterTitle, voiceFileExtension);


                    // dowload part
                    waitService.waitForSeconds(1);
                    googleAudioDownloaderService.download(audioUrl, audioFilePath);

                    // Add files to the list to merge later if needed
                    inputFilePaths.add(audioFilePath);

                    double duration = getAudioDurationInSeconds(audioFilePath);
                    double cursedNumber = 4.62;
                    double epsilonStrict = 0.0001;
                    double epsilonLoose = 0.01;

                    System.out.printf("🎧 Chapter %d - Audio duration: %.5f seconds%n", chapter.getChapterNumber(), duration);

                    double diff = Math.abs(duration - cursedNumber);

                    if (diff < epsilonStrict) {
                        new File(audioFilePath).delete();
                        throw new RuntimeException(String.format("📛 Chapter %d ❌ Exact 4.62s – known bad duration", chapter.getChapterNumber()));
                    } else if (diff < epsilonLoose) {
                        new File(audioFilePath).delete();
                        throw new RuntimeException(String.format("⚠️ Chapter %d ❗ Very close to 4.62s – suspicious duration", chapter.getChapterNumber()));
                    } else {
                        System.out.printf("✅ Chapter %d 🎧 Audio valid – continue!\n", chapter.getChapterNumber());
                    }

                    waitService.waitForSeconds(5);
                    driver.navigate().refresh();
                    waitService.waitForSeconds(5);
                }

                if (maxPingDeadNumber) {
                    throw new RuntimeException("Chapter " + chapter.getChapterNumber() + " fuck this dead number ");
                }

                System.out.println("✅ Text-to-speech conversion succeeded on attempt " + attempt);
                break;
            } catch (Exception e) {
                e.printStackTrace();

                // delete all
                for (String audioFilePaths : inputFilePaths) {
                    new File(audioFilePaths).delete();
                }

                if (attempt >= MAX_RETRIES) {
                    System.err.println("Chapter : " + chapter.getChapterNumber() + " " + e.getMessage());
                    throw new RuntimeException("❌ Tried " + MAX_RETRIES + " times but still failed to convert text to speech", e);
                }
                driver.navigate().refresh();
                waitService.waitForSeconds(5);
            }
        }

        /**
         * If there are multiple audio segments due to file content > 8000 characters, proceed to merge them.
         */
        if (inputFilePaths.size() > 1) {
            try {
                String randomKey = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                String mergeFilePath = novelDirectory + File.separator + randomKey + voiceFileExtension;

                audioMergerService.mergeChapterVideos(inputFilePaths, mergeFilePath);

                // Delete individual files, keep only merged files
                for (String filePath : inputFilePaths) {
                    System.out.println("Delete chunks file : " + filePath);
                    new File(filePath).delete();
                }

                // Rename merged file to match the first original filename with (1)
                String originalFirstPath = inputFilePaths.get(0);
                File originalFile = new File(originalFirstPath);
                String originalName = originalFile.getName();

                String renamedPath = originalFile.getParent() + File.separator + originalName;

                boolean renamed = new File(mergeFilePath).renameTo(new File(renamedPath));

                if (renamed) {
                    audioFilePath = renamedPath;
                    inputFilePaths.clear();
                } else {
                    audioFilePath = mergeFilePath;
                    System.err.println("⚠️ Failed to rename merged file, using random key name.");
                }


            } catch (Exception e) {
                System.err.println("Error when merging videos : " + e.getMessage());
            }
        }

        return new TextToSpeechResponseDTO("Successful conversion", textToSpeechUrl, audioUrl, audioFilePath);
    }

    private List<String> splitContentIntoChunks(String content) {
        int maxChunkSize = Integer.parseInt(propertiesService.getDefaultCharacterLimitTextToSpeechUrl());
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int totalLength = content.length();

        while (start < totalLength) {
            int end = Math.min(start + maxChunkSize, totalLength);

            if (end < totalLength) {
                int bestBreakPoint = findBestBreakPoint(content, start, end);
                chunks.add(content.substring(start, bestBreakPoint).trim());
                start = bestBreakPoint;
            } else {
                chunks.add(content.substring(start).trim());
                break;
            }
        }

        return chunks;
    }

    private int findBestBreakPoint(String content, int start, int end) {
        String punctuationMarks = ".!?…\n";

        for (int i = end; i > start; i--) {
            char c = content.charAt(i - 1);
            if (punctuationMarks.indexOf(c) != -1) {
                return i;
            }
        }

        for (int i = end; i > start; i--) {
            char c = content.charAt(i - 1);
            if (Character.isWhitespace(c)) {
                return i;
            }
        }

        return end;
    }

    private double getAudioDurationInSeconds(String filePath) {
        try {
            IsoFile isoFile = new IsoFile(filePath);
            MovieBox movieBox = isoFile.getMovieBox();
            if (movieBox == null || movieBox.getMovieHeaderBox() == null) {
                System.err.println("⚠️ File doesn't contain MovieBox or MovieHeaderBox");
                isoFile.close();
                return 0;
            }

            double duration = (double) movieBox.getMovieHeaderBox().getDuration()
                    / movieBox.getMovieHeaderBox().getTimescale();
            isoFile.close();
            return duration;
        } catch (IOException e) {
            System.err.println("❌ Failed to read audio duration: " + e.getMessage());
            return 0;
        }
    }

}

