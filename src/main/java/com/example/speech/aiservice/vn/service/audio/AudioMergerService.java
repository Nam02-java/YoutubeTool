package com.example.speech.aiservice.vn.service.audio;

import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class AudioMergerService {

    @Autowired
    private PropertiesService propertiesService;

    public void mergeChapterVideos(List<String> inputFiles, String outputFilePath) throws IOException {
        if (inputFiles == null || inputFiles.size() <= 1) return;

        String ffmpegPath = propertiesService.getFfmpegPath();

        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);

        // Add input files
        for (String input : inputFiles) {
            command.add("-i");
            command.add(input);
        }

        // Build filter_complex for audio-only concat
        int n = inputFiles.size();
        StringBuilder filter = new StringBuilder();
        for (int i = 0; i < n; i++) {
            filter.append("[").append(i).append(":0]");
        }
        filter.append("concat=n=").append(n).append(":v=0:a=1[out]");

        command.add("-filter_complex");
        command.add(filter.toString());
        command.add("-map");
        command.add("[out]");
        command.add("-y"); // Overwrite output
        command.add(outputFilePath);

        // Build and run the process
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // Merge stderr into stdout

        Process process = pb.start();

        // Read FFmpeg output (for debug)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[ffmpeg] " + line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("FFmpeg exited with code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg process was interrupted", e);
        }
    }
}