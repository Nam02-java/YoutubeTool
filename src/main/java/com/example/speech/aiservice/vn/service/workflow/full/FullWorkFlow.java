package com.example.speech.aiservice.vn.service.workflow.full;

import com.example.speech.aiservice.vn.dto.response.*;
import com.example.speech.aiservice.vn.model.entity.chapter.Chapter;
import com.example.speech.aiservice.vn.model.entity.novel.Novel;
import com.example.speech.aiservice.vn.service.image.ImageDesignService;
import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import com.example.speech.aiservice.vn.service.repositoryService.chapter.ChapterService;
import com.example.speech.aiservice.vn.service.crawl.WebCrawlerService;
import com.example.speech.aiservice.vn.service.google.GoogleChromeLauncherService;
import com.example.speech.aiservice.vn.service.google.WebDriverLauncherService;
import com.example.speech.aiservice.vn.service.speech.SpeechService;
import com.example.speech.aiservice.vn.service.video.VideoCreationService;
import com.example.speech.aiservice.vn.service.youtube.YoutubeUploadService;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Map;

@Service
@Scope("prototype") // Create a new instance every time you call
public class FullWorkFlow {
    private final GoogleChromeLauncherService googleChromeLauncherService;
    private final WebDriverLauncherService webDriverLauncherService;
    private final WebCrawlerService webCrawlerService;
    private final SpeechService speechService;
    private final VideoCreationService videoCreationService;
    private final YoutubeUploadService youtubeUploadService;
    private final ChapterService chapterService;
    private final PropertiesService propertiesService;
    private final ImageDesignService imageDesignService;

    // Constructor Injection
    @Autowired
    public FullWorkFlow(GoogleChromeLauncherService googleChromeLauncherService, WebDriverLauncherService webDriverLauncherService, WebCrawlerService webCrawlerService, SpeechService speechService, VideoCreationService videoCreationService, YoutubeUploadService youtubeUploadService, ChapterService chapterService, PropertiesService propertiesService, ImageDesignService imageDesignService) {
        this.googleChromeLauncherService = googleChromeLauncherService;
        this.webDriverLauncherService = webDriverLauncherService;
        this.webCrawlerService = webCrawlerService;
        this.speechService = speechService;
        this.videoCreationService = videoCreationService;
        this.youtubeUploadService = youtubeUploadService;
        this.chapterService = chapterService;
        this.propertiesService = propertiesService;
        this.imageDesignService = imageDesignService;
    }

    public void runProcess(String port, String seleniumFileName, Novel novel, String chapterLinkToScan, String imagePath,
                           Map<Integer, String> videoPathMap, Map<String, int[]> totalVideoMap, int maxChapterNumber) {
        FullProcessResponseDTO fullProcessResponseDTO = fullProcessResponseDTO(
                port, seleniumFileName, novel, chapterLinkToScan, imagePath,
                videoPathMap, totalVideoMap, maxChapterNumber
        );
    }

    private FullProcessResponseDTO fullProcessResponseDTO(String port, String seleniumFileName, Novel novel, String chapterLinkToScan, String imagePath,
                                                          Map<Integer, String> videoPathMap, Map<String, int[]> totalVideoMap, int maxChapterNumber) {
        WebDriver chromeDriver = null;
        Process chromeProcess = null;
        try {

            chromeProcess = googleChromeLauncherService.openGoogleChrome(port, seleniumFileName);
            chromeDriver = webDriverLauncherService.initWebDriver(port);

            // Crawl data on Chivi.App website
            WebCrawlResponseDTO webCrawlResponseDTO = webCrawlerService.webCrawlResponseDTO(chromeDriver, novel, chapterLinkToScan, maxChapterNumber);

            // Save chapter to database
            saveChapterToDatabase(novel, webCrawlResponseDTO, chapterLinkToScan);

            Chapter chapter = chapterService.getChapterByLink(chapterLinkToScan);

            // Convert text to speech with ADMICRO | Vietnamese Speech Synthesis
            TextToSpeechResponseDTO textToSpeechResponseDTO = speechService.textToSpeechResponseDTO(chromeDriver, webCrawlResponseDTO.getContentFilePath(), novel, chapter);

            webDriverLauncherService.shutDown(chromeDriver);
            googleChromeLauncherService.shutdown(chromeProcess);

            /**
             * create chapter image path
             */
            String chapterImagePath = imageDesignService.installChapterFont(imagePath, novel, chapter, totalVideoMap);

            // Create videos using mp4 files combined with photos
            CreateVideoResponseDTO createVideoResponseDTO = videoCreationService.createVideoResponseDTO(textToSpeechResponseDTO.getFilePath(), chapterImagePath, novel, chapter);

            int chapterNumber = chapter.getChapterNumber();
            String videoFilePath = createVideoResponseDTO.getCreatedVideoFilePath();
            videoPathMap.put(chapterNumber, videoFilePath);

            //Upload video to youtube with youtube data API
            //YoutubeUploadResponseDTO youtubeUploadResponseDTO = youtubeUploadService.upload(createVideoResponseDTO.getCreatedVideoFilePath(), novel, chapter,totalChapterNumber);

            // Aggregated DTO response
            FullProcessResponseDTO fullProcessResponse = new FullProcessResponseDTO(webCrawlResponseDTO, textToSpeechResponseDTO, createVideoResponseDTO, null);

            chapterService.markChapterAsScanned(chapter);

            return fullProcessResponse;

        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {
            webDriverLauncherService.shutDown(chromeDriver);
            googleChromeLauncherService.shutdown(chromeProcess);
        }
    }

    private void saveChapterToDatabase(Novel novel, WebCrawlResponseDTO webCrawlResponseDTO, String chapterLinkToScan) {
        if (!chapterService.isExistsByNovelAndChapterNumber(novel, webCrawlResponseDTO.getChapterNumber())) {
            chapterService.addChapter(novel, webCrawlResponseDTO.getChapterNumber(), webCrawlResponseDTO.getTitle(), chapterLinkToScan);
        } else {
            System.out.println(String.format("😢 %s (Chapter: %s) - URL: %s already exists in the database.",
                    webCrawlResponseDTO.getTitle(),
                    webCrawlResponseDTO.getChapterNumber(),
                    webCrawlResponseDTO.getUrlWebsite()));
        }
    }
}

