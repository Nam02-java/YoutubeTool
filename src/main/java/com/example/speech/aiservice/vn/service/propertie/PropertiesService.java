package com.example.speech.aiservice.vn.service.propertie;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@Service
public class PropertiesService {

    private final Environment environment;

    @Autowired
    public PropertiesService(Environment environment) {
        this.environment = environment;
    }

    public String getImageDirectory() {
        return environment.getProperty("app.image.directory");
    }

    public String getDefaultPort() {
        return environment.getProperty("app.default.port");
    }

    public String getImageExtension() {
        return environment.getProperty("app.image.extension");
    }

    public String getHomePageChiviAppUrl() {
        return environment.getProperty("app.url.homepage.chivi.app");
    }

    public String getHomePageIxdzs8Url() {
        return environment.getProperty("app.url.homepage.ixdzs8");
    }

    public String getHomePage038kURL() {
        return environment.getProperty("app.url.homepage.038k");
    }

    public String getTextToSpeechUrl() {
        return environment.getProperty("app.url.text-to-speech");
    }

    public String getDefaultCharacterLimitTextToSpeechUrl() {
        return environment.getProperty("app.url.text-to-speech.default.character.limit");
    }

    public String getChromePath() {
        return environment.getProperty("app.chrome.path");
    }

    public String getUserDataDir() {
        return environment.getProperty("app.chrome.user-data-dir");
    }

    public String getContentDirectory() {
        return environment.getProperty("app.content.directory");
    }

    public String getContentFileExtension() {
        return environment.getProperty("app.content.extension");
    }

    public String getFfmpegPath() {
        return environment.getProperty("app.ffmpeg.path");
    }

    public String getVoiceDirectory() {
        return environment.getProperty("app.voice.directory");
    }

    public String getVoiceFileExtension() {
        return environment.getProperty("app.voice.file-extension");
    }

    public String getUploadDirectory() {
        return environment.getProperty("app.upload.directory");
    }

    public String getChapterVideos() {
        return environment.getProperty("app.chapter.videos.directory");
    }

    public String getUploadFileExtension() {
        return environment.getProperty("app.upload.file-extension");
    }

    public String getOAuthClientSecretFile() {
        return environment.getProperty("app.oauth.client-secret-file");
    }

    public String getOAuthTokensDirectory() {
        return environment.getProperty("app.oauth.tokens-directory");
    }

    public String getChapterImageDirectory() {
        return environment.getProperty("app.chapter.image.directory");
    }

    public String getBannerFontPath(String fontKey) {
        String propertyKey = "app.banner.font." + fontKey + ".path";
        return environment.getProperty(propertyKey);
    }

    public String getTotalChapterPerVideo() {
        return environment.getProperty("app.total.chapters.per.video");
    }

    public String getEsrganExePath() {
        return environment.getProperty("app.esrgan.exe-path");
    }

    public String getEsrganModelDirectory() {
        return environment.getProperty("app.esrgan.model-directory");
    }

    public String getEsrganModelName() {
        return environment.getProperty("app.esrgan.model-name");
    }

    public String getEsrganOutputPath() {
        return environment.getProperty("app.esrgan.output-path");
    }

    public String getThumbnailDirectory() {
        return environment.getProperty("app.thumbnail.directory");
    }

    public String getBaloo2BoldPath() {
        return environment.getProperty("app.banner.font.baloo2-bold.path");
    }
}


