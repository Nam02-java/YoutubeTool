package com.example.speech.aiservice.vn.service.executor;

import com.example.speech.aiservice.vn.model.entity.novel.Novel;
import com.example.speech.aiservice.vn.service.workflow.full.FullWorkFlow;

import java.awt.*;
import java.util.*;

public class MyRunnableService implements Runnable {
    private final FullWorkFlow fullWorkFlow;
    private final String port;
    private final String seleniumFileName;
    private final Novel novel;
    private final String chapterLinkToScan;
    private final String imagePath;
    private final Map<Integer, String> videoPathMap;
    private Map<String, int[]> totalVideoMap = new LinkedHashMap<>();

    private int maxChapterNumber;

    public MyRunnableService(
            FullWorkFlow fullWorkFlow,
            String port,
            String seleniumFileName,
            Novel novel,
            String chapterLinkToScan,
            String imagePath,
            Map<Integer, String> videoPathMap,
            Map<String, int[]> totalVideoMap,
            int maxChapterNumber


    ) {
        this.fullWorkFlow = fullWorkFlow;
        this.port = port;
        this.seleniumFileName = seleniumFileName;
        this.novel = novel;
        this.chapterLinkToScan = chapterLinkToScan;
        this.imagePath = imagePath;
        this.videoPathMap = videoPathMap;
        this.totalVideoMap = totalVideoMap;
        this.maxChapterNumber = maxChapterNumber;
    }

    @Override
    public void run() {
        System.out.println("▶️ Running on thread: " + Thread.currentThread().getId());

        fullWorkFlow.runProcess(
                port,
                seleniumFileName,
                novel,
                chapterLinkToScan,
                imagePath,
                videoPathMap,
                totalVideoMap,
                maxChapterNumber
        );
    }
}

