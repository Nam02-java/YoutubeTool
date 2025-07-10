package com.example.speech.aiservice.vn.dto.response;

public class NovelInfoResponseDTO {
    private String title;
    private String storyTitle;
    private String link;

    public NovelInfoResponseDTO(String title, String storyTitle, String link) {
        this.title = title;
        this.storyTitle = storyTitle;
        this.link = link;
    }

    public String getTitle() {
        return title;
    }

    public String getStoryTitle() {
        return storyTitle;
    }

    public String getLink() {
        return link;
    }


    @Override
    public String toString() {
        return "Title: " + title + "\nStory title : " + storyTitle + "\nLink: " + link;
    }
}
