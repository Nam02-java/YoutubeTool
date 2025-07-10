package com.example.speech.aiservice.vn.service.string;

import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class NovelLinkBuilderService {

    private final PropertiesService propertiesService;

    public NovelLinkBuilderService(PropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    public String buildNovelLink(String url) {
        String ixdzs8HomePageUrl = propertiesService.getHomePageIxdzs8Url(); // Example: https://ixdzs8.com/
        String homePage038kUrl = propertiesService.getHomePage038kURL(); // Example: http://www.038k.com/

        if (url.contains(ixdzs8HomePageUrl) ) {
            URI uri = null;
            try {
                uri = new URI(url);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            String domain = uri.getHost().replace(".", "-"); // ixdzs8-com
            String translatedDomain = domain + ".translate.goog";

            String path = uri.getPath(); // Ví dụ: /read/396056/p27.html
            String query = uri.getQuery() != null ? "?" + uri.getQuery() : "";
            String translatedLink = "https://" + translatedDomain + path + query
                    + (query.isEmpty() ? "?" : "&") + "_x_tr_sl=zh-CN&_x_tr_tl=vi&_x_tr_hl=vi";

            System.out.println("✅ " + uri+  " website has been translated via .translate.goog: " + translatedLink);
            url = translatedLink;
        }
        return url;
    }
}
