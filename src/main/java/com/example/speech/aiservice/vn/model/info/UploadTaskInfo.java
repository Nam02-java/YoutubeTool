package com.example.speech.aiservice.vn.model.info;

public class UploadTaskInfo {
    private final String videoPath;
    private final String title;
    private final String description;
    private final String playListName;
    private final String imagePath;
    private final String rangeChapter;

    public UploadTaskInfo(String videoPath, String title, String description,
                          String playListName, String imagePath, String rangeChapter) {
        this.videoPath = videoPath;
        this.title = title;
        this.description = description;
        this.playListName = playListName;
        this.imagePath = imagePath;
        this.rangeChapter = rangeChapter;
    }

    // Getters
    public String getVideoPath() {
        return videoPath;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getPlayListName() {
        return playListName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getRangeChapter() {
        return rangeChapter;
    }
}
