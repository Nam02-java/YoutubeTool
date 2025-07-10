package com.example.speech.aiservice.vn.service.crawl;

import com.example.speech.aiservice.vn.dto.response.WebCrawlResponseDTO;
import com.example.speech.aiservice.vn.model.entity.novel.Novel;
import com.example.speech.aiservice.vn.service.filehandler.FileNameService;
import com.example.speech.aiservice.vn.service.filehandler.FileWriterService;
import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import com.example.speech.aiservice.vn.service.repositoryService.chapter.ChapterService;
import com.example.speech.aiservice.vn.service.string.ChapterLinkBuilderService;
import com.example.speech.aiservice.vn.service.wait.WaitService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;


@Service
public class WebCrawlerService {
    private final FileNameService fileNameService;
    private final FileWriterService fileWriterService;
    private final WaitService waitService;
    private final PropertiesService propertiesService;
    private final ChapterService chapterService;
    private final ChapterLinkBuilderService chapterLinkBuilderService;

    @Autowired
    public WebCrawlerService(FileNameService fileNameService, FileWriterService fileWriterService, WaitService waitService, PropertiesService propertiesService, ChapterService chapterService, ChapterLinkBuilderService chapterLinkBuilderService) {
        this.fileNameService = fileNameService;
        this.fileWriterService = fileWriterService;
        this.waitService = waitService;
        this.propertiesService = propertiesService;
        this.chapterService = chapterService;
        this.chapterLinkBuilderService = chapterLinkBuilderService;
    }

    public WebCrawlResponseDTO webCrawlResponseDTO(WebDriver driver, Novel novel, String chapterLinkToScan, int chapterNumber) throws InterruptedException {

        String homePageIxdzs8Url = propertiesService.getHomePageIxdzs8Url();
        String homePage038kURL = propertiesService.getHomePage038kURL();

        String contentDirectoryPath = propertiesService.getContentDirectory();
        String contentFileExtension = propertiesService.getContentFileExtension();

        driver.get(chapterLinkToScan);


        // it's work ! don't change pls !
        waitService.waitForSeconds(5);
        driver.navigate().refresh();
        waitService.waitForSeconds(5);

        String pageSource = driver.getPageSource();

        Document doc = Jsoup.parse(pageSource);

        final int MAX_RETRIES = 10;
        int attempt = 0;
        String title = null;

        while (attempt < MAX_RETRIES) {
            attempt++;
            System.out.println("Thread ID : " + Thread.currentThread().getId() + " 🌀 Attempt " + attempt + " / " + MAX_RETRIES);

            if (chapterLinkToScan.contains(homePageIxdzs8Url)) {
                title = doc.select("#page > article > h3").text();
            } else if (chapterLinkToScan.contains(homePage038kURL)) {
                title = doc.select("#container > div > div > div.reader-main > h1 > font > font").text();
            }

            if (title != null && !title.isBlank()) {
                System.out.println("✅ Found title: " + title);
                break;
            }

            System.err.println("⚠️ Title empty, retrying..." + " " + chapterLinkToScan);
            driver.navigate().refresh();
            waitService.waitForSeconds(5);
            pageSource = driver.getPageSource();
            doc = Jsoup.parse(pageSource);
        }


        String content = null;
        String fullContent = null;
        if (chapterLinkToScan.contains(homePageIxdzs8Url)) {
            content = doc.select("#page > article").text();
        } else if (chapterLinkToScan.contains(homePage038kURL)) {
            content = doc.select("#content").text();
            fullContent = title + "," + content;
        }

        String noChinese = fullContent.replaceAll("\\p{IsHan}", "");
        String cleanedContent = noChinese.replaceAll("[^a-zA-ZÀ-ỹ0-9,. ]+", "");
        cleanedContent = cleanedContent.replaceAll("\\s+", " ").trim();

        System.out.println("Title : " + title);
        System.out.println("Chapter number : " + chapterNumber);
        System.out.println("Content : " + cleanedContent);


        // Create a folder for the collection if it does not exist.
        String safeNovelTitle = fileNameService.sanitizeFileName(novel.getTitle());
        String novelDirectory = contentDirectoryPath + File.separator + safeNovelTitle;
        fileNameService.ensureDirectoryExists(novelDirectory);

        // Handling valid chapter file names
        String safeChapterTitle = fileNameService.sanitizeFileName(title) + contentFileExtension;
        String contentFilePath = fileNameService.getAvailableFileName(novelDirectory, safeChapterTitle, contentFileExtension);

        // Write content to file
        fileWriterService.writeToFile(contentFilePath, cleanedContent);

        return new WebCrawlResponseDTO("Crawling completed", title, chapterNumber, chapterLinkToScan, contentFilePath);
    }
}

