package com.example.speech.aiservice.vn.service.youtube;

import com.example.speech.aiservice.vn.model.entity.novel.Novel;
import com.example.speech.aiservice.vn.service.date.Calculate;
import com.example.speech.aiservice.vn.service.filehandler.FileNameService;
import com.example.speech.aiservice.vn.service.image.ImageDesignService;
import com.example.speech.aiservice.vn.service.image.ImageResizeService;
import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import com.example.speech.aiservice.vn.service.repositoryService.chapter.ChapterService;
import com.example.speech.aiservice.vn.service.wait.WaitService;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Service
public class YouTubeUploader {

    private final OAuthHelper oAuthHelper;
    private final WaitService waitService;
    private final PlaylistSplitter playlistSplitter;
    private final ChapterService chapterService;
    private final ImageResizeService imageResizeService;
    private final PropertiesService propertiesService;
    private final FileNameService fileNameService;
    private final ImageDesignService imageDesignService;
    private final Calculate calculate;

    @Autowired
    public YouTubeUploader(
            OAuthHelper oAuthHelper,
            WaitService waitService,
            PlaylistSplitter playlistSplitter,
            ChapterService chapterService,
            ImageResizeService imageResizeService,
            PropertiesService propertiesService,
            FileNameService fileNameService,
            ImageDesignService imageDesignService, Calculate calculate) {
        this.oAuthHelper = oAuthHelper;
        this.waitService = waitService;
        this.playlistSplitter = playlistSplitter;
        this.chapterService = chapterService;
        this.imageResizeService = imageResizeService;
        this.propertiesService = propertiesService;
        this.fileNameService = fileNameService;
        this.imageDesignService = imageDesignService;
        this.calculate = calculate;
    }


    public String uploadVideo(String inputLink,
                              Novel novel,
                              String videoFilePath,
                              String title, String description,
                              String tags,
                              String privacyStatus,
                              String playListName,
                              String imagePath,
                              String rangeChapter) throws Exception {
        YouTube youtubeService = oAuthHelper.getService();

        // Configure video metadata
        Video video = new Video();
        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus(privacyStatus);

        // ========== Step 2: Set publishAt at 18:00 VN time ==========
        DateTime publishAt = calculate.getNextVietnamPublishTimeUTC(inputLink);

        Instant instant = Instant.ofEpochMilli(publishAt.getValue());
        ZonedDateTime vnTime = instant.atZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        System.out.println("🇻🇳 Vietnam Time Upload Video : " + vnTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        status.setPublishAt(publishAt);

        video.setStatus(status);

        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle(title);
        snippet.setDescription(description);
        snippet.setTags(Collections.singletonList(tags));
        video.setSnippet(snippet);

        File mediaFile = new File(videoFilePath);
        FileContent mediaContent = new FileContent("video/*", mediaFile);

        // Send upload request
        while (true) {
            try {
                YouTube.Videos.Insert request = youtubeService.videos().insert("snippet,status", video, mediaContent);
                Video response = request.execute();
                String videoId = response.getId();
                String uploadedVideoURL = "https://www.youtube.com/watch?v=" + videoId;
                System.out.printf("%s - uploaded at: %s%n", title, uploadedVideoURL);

                /**
                 * new update
                 * playlist
                 */
                String playlistId = getPlaylistId(youtubeService, playListName);

                PlaylistItem playlistItem = new PlaylistItem();
                PlaylistItemSnippet itemSnippet = new PlaylistItemSnippet();
                itemSnippet.setPlaylistId(playlistId);
                itemSnippet.setResourceId(new ResourceId().setKind("youtube#video").setVideoId(videoId));
                playlistItem.setSnippet(itemSnippet);

                youtubeService.playlistItems()
                        .insert("snippet", playlistItem)
                        .execute();

                System.out.println("🎬 Video has been added to playlist: "
                        + playListName
                        + " (ID: " + playlistId + ")"
                        + "\n📺 Link: https://www.youtube.com/playlist?list=" + playlistId);

                String thumbnailYoutubeDirectoryPath = propertiesService.getThumbnailDirectory();
                String imageExtension = propertiesService.getImageExtension();

                // Create a folder for the collection if it does not exist.
                String safeNovelTitle = fileNameService.sanitizeFileName(novel.getTitle());
                String thumbnailDirectory = thumbnailYoutubeDirectoryPath + File.separator + safeNovelTitle;
                fileNameService.ensureDirectoryExists(thumbnailDirectory);


                // Handling valid chapter file names
                String safeThumbnailTitle = fileNameService.sanitizeFileName(title);
                String thumbnailFilePath = fileNameService.getAvailableFileNameWithNoNumber(thumbnailDirectory, safeThumbnailTitle, imageExtension);

                if (!Files.exists(Path.of(thumbnailFilePath))) {
                    Path source = Paths.get(imagePath);
                    Path target = Paths.get(thumbnailFilePath);
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }

                // YouTube thumbnail standard size
                int targetWidth = 1280;
                int targetHeight = 720;
                String resizeImage = imageResizeService.excute(thumbnailFilePath, targetWidth, targetHeight);
                String finalImagePath = imageDesignService.installRangeChapter(resizeImage, rangeChapter);
                setVideoThumbnail(youtubeService, videoId, finalImagePath);

                return uploadedVideoURL;
            } catch (GoogleJsonResponseException e) {
                if (e.getDetails() != null && e.getDetails().getErrors() != null) {
                    boolean retry = false;
                    for (GoogleJsonError.ErrorInfo error : e.getDetails().getErrors()) {
                        if ("uploadLimitExceeded".equals(error.getReason())) {
                            e.printStackTrace();
                            System.out.println("Upload limit exceeded. Retrying in 10 minutes...");
                            waitService.waitForSeconds(600); // default wait 10 minutes
                            retry = true;
                            break;
                        }
                    }

                    /**
                     * Continue looping if upload error Limit Exceeded
                     * Continue until video upload is successful
                     */
                    if (retry) {
                        continue;
                    }
                }
                throw e; // If other error, exit immediately
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }


    private synchronized String getPlaylistId(YouTube youtubeService, String playlistTitle) throws IOException {
        // Find playlists with duplicate names
        YouTube.Playlists.List playlistRequest = youtubeService.playlists()
                .list("snippet")
                .setMine(true)
                .setMaxResults(50L);
        PlaylistListResponse response = playlistRequest.execute();
        for (Playlist playlist : response.getItems()) {
            if (playlist.getSnippet().getTitle().equalsIgnoreCase(playlistTitle)) {
                return playlist.getId(); // Already exists
            }
        }

        // If not, create a new one playlist
        Playlist newPlaylist = new Playlist();
        PlaylistSnippet snippet = new PlaylistSnippet();
        snippet.setTitle(playlistTitle);
        snippet.setDescription(playlistTitle);
        newPlaylist.setSnippet(snippet);
        newPlaylist.setStatus(new PlaylistStatus().setPrivacyStatus("public")); //  "unlisted", "private"

        Playlist inserted = youtubeService.playlists()
                .insert("snippet,status", newPlaylist)
                .execute();

        return inserted.getId();
    }

    private void setVideoThumbnail(YouTube youtubeService, String videoId, String thumbnailPath) throws IOException {
        File thumbnailFile = new File(thumbnailPath);
        FileContent mediaContent = new FileContent("image/png", thumbnailFile); // hoặc "image/jpeg"

        YouTube.Thumbnails.Set thumbnailSet = youtubeService.thumbnails()
                .set(videoId, mediaContent);
        ThumbnailSetResponse setResponse = thumbnailSet.execute();

        System.out.println("✅ Thumbnail set for video: " + videoId);
    }
}
