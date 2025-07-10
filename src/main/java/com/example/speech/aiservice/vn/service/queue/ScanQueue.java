package com.example.speech.aiservice.vn.service.queue;

import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import com.example.speech.aiservice.vn.service.string.ChapterLinkBuilderService;
import com.example.speech.aiservice.vn.service.string.NovelLinkBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component // Integrated into Spring ecosystem
public class ScanQueue {
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final PropertiesService propertiesService;
    private final NovelLinkBuilderService novelLinkBuilderService;


    @Autowired
    public ScanQueue(PropertiesService propertiesService, NovelLinkBuilderService novelLinkBuilderService) {
        this.propertiesService = propertiesService;
        this.novelLinkBuilderService = novelLinkBuilderService;
    }

    public void addToQueue(String url) {
        url = novelLinkBuilderService.buildNovelLink(url);
        try {
            queue.put(url);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public String takeFromQueue() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Failed to take from queue: " + e.getMessage());
            return null;
        }
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void printQueue() {
        System.out.println("Current Queue: " + Arrays.toString(queue.toArray()));
    }
}
