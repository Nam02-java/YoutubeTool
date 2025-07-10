package com.example.speech.aiservice.vn.service.youtube;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class PlaylistSplitter {

    public  List<String> splitChaptersIntoPlaylists(int totalChapters) {
        List<String> playlists = new ArrayList<>();
        int currentChapter = 1;

        int groupCount;


        if (totalChapters <= 50) {
            // <= 50: put all into 1 playlist
            int start = currentChapter;
            int end = totalChapters;
            playlists.add("Chapter " + start + "–" + end);
            return playlists;

        }if (totalChapters <= 100) {
            // <= 100: split into 3 playlist
            groupCount = 3;

        } else if (totalChapters <= 400) {
            // <= 400: split into 20 playlist
            groupCount = 20;

        } else {
            // > 400: split into 10 playlist
            groupCount = 10;
        }

        int chaptersPerGroup = totalChapters / groupCount;
        int remainingChapters = totalChapters % groupCount;

        for (int i = 1; i <= groupCount; i++) {
            int start = currentChapter;
            int end = currentChapter + chaptersPerGroup - 1;

            // Move remaining chapters to the last playlist
            if (i == groupCount) {
                end += remainingChapters;
            }

            playlists.add("Chapter " + start + "–" + end);
            currentChapter = end + 1;
        }

        return playlists;
    }
}
