package com.example.speech.aiservice.vn.service.video;

import com.example.speech.aiservice.vn.dto.response.CreateVideoResponseDTO;
import com.example.speech.aiservice.vn.model.entity.chapter.Chapter;
import com.example.speech.aiservice.vn.model.entity.novel.Novel;
import com.example.speech.aiservice.vn.service.filehandler.FileNameService;
import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import org.mp4parser.IsoFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class VideoCreationService {

    private final FileNameService fileNameService;
    private final PropertiesService propertiesService;

    @Autowired
    public VideoCreationService(FileNameService fileNameService, PropertiesService propertiesService) {
        this.fileNameService = fileNameService;
        this.propertiesService = propertiesService;
    }

    /**
     * for window laptop
     *
     * @param audioPath
     * @param imagePath
     * @param novel
     * @param chapter
     * @return
     */
    public CreateVideoResponseDTO createVideoResponseDTO(String audioPath, String imagePath, Novel novel, Chapter chapter) {

        String ffmpegPath = propertiesService.getFfmpegPath();
        String chapterVideosDirectoryPath = propertiesService.getChapterVideos();
        String uploadFileExtension = propertiesService.getUploadFileExtension();

        if (audioPath == null) {
            System.out.println("⚠️ No video files found!");
            return new CreateVideoResponseDTO("⚠️ No video files found!", null, null, null);
        }

        // Create a directory for the exploit if it does not exist
        String safeNovelTitle = fileNameService.sanitizeFileName(novel.getTitle());
        String novelDirectory = chapterVideosDirectoryPath + File.separator + safeNovelTitle;
        fileNameService.ensureDirectoryExists(novelDirectory);

        // Handling valid chapter file names
        String safeChapterTitle = fileNameService.sanitizeFileName(chapter.getTitle());
        safeChapterTitle = chapter.getChapterNumber() + "." + safeChapterTitle;
        String videoFilePath = fileNameService.getAvailableFileName(novelDirectory, safeChapterTitle, uploadFileExtension);

        String duration = getMp4AudioDuration(audioPath);
        System.out.println("🎯 Audio duration: " + duration);

        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-loop");
        command.add("1");
        command.add("-framerate");
        command.add("1");
        command.add("-i");
        command.add(imagePath);
        command.add("-i");
        command.add(audioPath);
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("ultrafast");
        command.add("-tune");
        command.add("stillimage");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("192k");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-t");
        command.add(duration);
        command.add(videoFilePath);

        System.out.println("Run command : " + String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("✅ Complete combining photos into video! Output file : " + videoFilePath);
            } else {
                System.out.println("⚠️ FFmpeg encountered an error, check the output above.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return new CreateVideoResponseDTO("Create a successful video", imagePath, audioPath, videoFilePath);
    }


    public String getMp4AudioDuration(String path) {
        try {
            IsoFile isoFile = new IsoFile(path);
            double lengthInSeconds =
                    (double) isoFile.getMovieBox().getMovieHeaderBox().getDuration() /
                            isoFile.getMovieBox().getMovieHeaderBox().getTimescale();
            isoFile.close();
            System.out.println("🎯 Duration (mp4parser): " + lengthInSeconds);
            return String.valueOf(lengthInSeconds);
        } catch (IOException e) {
            e.printStackTrace();
            return "0";
        }
    }


    /**
     * for ubuntu desktop
     *
     * @param audioPath
     * @param imagePath
     * @param novel
     * @param chapter
     * @return
     */
//    public CreateVideoResponseDTO createVideoResponseDTO(String audioPath, String imagePath, Novel novel, Chapter chapter) {
//
//        String ffmpegPath = propertiesService.getFfmpegPath();
//        String uploadDirectoryPath = propertiesService.getUploadDirectory();
//        String uploadFileExtension = propertiesService.getUploadFileExtension();
//
//        if (audioPath == null) {
//            System.out.println("⚠️ No video files found!");
//            return new CreateVideoResponseDTO("⚠️ No video files found!", null, null, null);
//        }
//
//
//        // Create a directory for the exploit if it does not exist
//        String safeNovelTitle = fileNameService.sanitizeFileName(novel.getTitle());
//        String novelDirectory = uploadDirectoryPath + File.separator + safeNovelTitle;
//        fileNameService.ensureDirectoryExists(novelDirectory);
//
//
//        // Handling valid chapter file names
//        String safeChapterTitle = fileNameService.sanitizeFileName(chapter.getTitle());
//        //String audioFilePath = novelDirectory + File.separator + safeChapterTitle;
//        String videoFilePath = fileNameService.getAvailableFileName(novelDirectory, safeChapterTitle, uploadFileExtension);
//
//        // FFmpeg command
//        List<String> command = Arrays.asList(
//                ffmpegPath,
//                "-loop", "1",
//                "-i", imagePath,
//                "-i", audioPath,
//                "-c:v", "libx264",
//                "-tune", "stillimage",
//                "-c:a", "aac",
//                "-b:a", "192k",
//                "-pix_fmt", "yuv420p",
//                "-shortest",
//                videoFilePath
//        );
//
//        System.out.println("Executing command: " + command);
//        System.out.println("Run command: " + command);
//        try {
//            ProcessBuilder pb = new ProcessBuilder(command);
//            pb.redirectErrorStream(true);
//            Process process = pb.start();
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
//            }
//
//            int exitCode = process.waitFor();
//            if (exitCode == 0) {
//                System.out.println("✅ Complete! Output file: " + videoFilePath);
//            } else {
//                System.out.println("⚠️ FFmpeg encountered an error.");
//            }
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
//        return new CreateVideoResponseDTO("Create a successful video", imagePath, audioPath, videoFilePath);
//    }
}
