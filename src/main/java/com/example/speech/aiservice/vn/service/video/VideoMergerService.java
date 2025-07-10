package com.example.speech.aiservice.vn.service.video;

import com.example.speech.aiservice.vn.model.entity.novel.Novel;
import com.example.speech.aiservice.vn.service.filehandler.FileNameService;
import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;


@Service
public class VideoMergerService {

    private final PropertiesService propertiesService;
    private final FileNameService fileNameService;

    @Autowired
    public VideoMergerService(PropertiesService propertiesService, FileNameService fileNameService) {
        this.propertiesService = propertiesService;
        this.fileNameService = fileNameService;
    }

    public String mergeVideos(List<String> inputVideoPaths, Novel novel, String videoName) throws IOException, InterruptedException {
        if (inputVideoPaths == null || inputVideoPaths.isEmpty()) return null;

        String uploadVideoDirectoryPath = propertiesService.getUploadDirectory();

        // Create a directory for the exploit if it does not exist
        String safeNovelTitle = fileNameService.sanitizeFileName(novel.getTitle());
        String novelDirectory = uploadVideoDirectoryPath + File.separator + safeNovelTitle;
        String uploadFileExtension = propertiesService.getUploadFileExtension();

        fileNameService.ensureDirectoryExists(novelDirectory);

        // Handling valid chapter file names
        String safeVideoTitle = fileNameService.sanitizeFileName(videoName);
        String videoFilePath = fileNameService.getAvailableFileName(novelDirectory, safeVideoTitle, uploadFileExtension);

        // If there is only 1 video, copy that file to the destination location.
        if (inputVideoPaths.size() == 1) {
            Files.copy(Paths.get(inputVideoPaths.get(0)), Paths.get(videoFilePath), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ Only 1 video, copied directly: " + videoFilePath);
            return videoFilePath;
        }

        String ffmpegPath = propertiesService.getFfmpegPath();

        // Wỉre list file into ffmpeg_list.txt
        File listFile = new File(System.getProperty("java.io.tmpdir"), "ffmpeg_list.txt");

        try (PrintWriter writer = new PrintWriter(listFile)) {
            for (String path : inputVideoPaths) {
                writer.println("file '" + path.replace("'", "'\\''") + "'");
            }
        }

        // FFmpeg command to merge videos (fastest - copy stream)
        List<String> command = Arrays.asList(
                ffmpegPath, "-f", "concat", "-safe", "0",
                "-i", listFile.getAbsolutePath(),
                "-c", "copy",
                "-y", videoFilePath
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[ffmpeg] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg merge failed with exit code: " + exitCode);
        }
        System.out.println("✅ The video has been merged: " + videoFilePath);

        return videoFilePath;
    }
}
