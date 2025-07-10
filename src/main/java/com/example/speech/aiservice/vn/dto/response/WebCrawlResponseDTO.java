package com.example.speech.aiservice.vn.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//@Getter
@Setter
@NoArgsConstructor
//@AllArgsConstructor
public class WebCrawlResponseDTO {

    @JsonProperty("message")
    private String message;

    @JsonProperty("title")
    private String title;

    @JsonProperty("chapter_number")
    private int chapterNumber;

    @JsonProperty("url_website")
    private String urlWebsite;

    @JsonProperty("content_file_path")
    private String contentFilePath;

    public WebCrawlResponseDTO(String message, String title, int chapterNumber, String urlWebsite, String contentFilePath) {
        this.message = message;
        this.title = title;
        this.chapterNumber = chapterNumber;
        this.urlWebsite = urlWebsite;
        this.contentFilePath = contentFilePath;
    }


    public String getMessage() {
        return message;
    }

    public String getTitle() {
        return title;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public String getUrlWebsite() {
        return urlWebsite;
    }

    public String getContentFilePath() {
        return contentFilePath;
    }

}
