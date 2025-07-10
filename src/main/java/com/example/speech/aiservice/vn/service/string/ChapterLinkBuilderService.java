package com.example.speech.aiservice.vn.service.string;

import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChapterLinkBuilderService {

    private final PropertiesService propertiesService;

    public ChapterLinkBuilderService(PropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    public String buildChapterLink(String baseLink, int chapterNumber) {
        // Remove any trailing slash before the query params
        int queryIndex = baseLink.indexOf('?');
        String linkWithoutQuery = (queryIndex != -1) ? baseLink.substring(0, queryIndex) : baseLink;
        String queryParams = (queryIndex != -1) ? baseLink.substring(queryIndex) : "";

        // Ensure no trailing slash
        if (linkWithoutQuery.endsWith("/")) {
            linkWithoutQuery = linkWithoutQuery.substring(0, linkWithoutQuery.length() - 1);
        }

        // Extract novel ID
        int lastSlashIndex = linkWithoutQuery.lastIndexOf('/');
        String beforeId = linkWithoutQuery.substring(0, lastSlashIndex); // https://ixdzs8-com.translate.goog/read
        String novelIdPart = linkWithoutQuery.substring(lastSlashIndex + 1); // 396056

        return beforeId + "/" + novelIdPart + "/p" + chapterNumber + ".html" + queryParams;
    }



    public int extractChapterNumberFromUrl(String url) {
        Pattern pattern = Pattern.compile("/p(\\d+)\\.html");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new RuntimeException("Không tìm thấy số chương trong URL: " + url);
    }
}
