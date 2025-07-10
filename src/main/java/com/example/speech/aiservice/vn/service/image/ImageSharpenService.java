package com.example.speech.aiservice.vn.service.image;

import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class ImageSharpenService {

    private final PropertiesService propertiesService;

    @Autowired
    public ImageSharpenService(PropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    public String excute(String inputImagePath) {

        String exeFolder = propertiesService.getEsrganExePath();
        String modelFolder = propertiesService.getEsrganModelDirectory();
        String modelName = propertiesService.getEsrganModelName();


        // RealESRGAN-ncnn-vulkan run command
        ProcessBuilder pb = new ProcessBuilder(
                exeFolder + "\\RealESRGAN-ncnn-vulkan.exe",
                "-i", inputImagePath, // Input
                "-o", inputImagePath, // Output (overwrite)
                "-n", modelName,
                "-m", modelFolder,
                "-g", "0" // 0 = CPU, -1 = GPU if Vulkan is present
        );

        // Work Directory
        pb.directory(new File(exeFolder));
        pb.redirectErrorStream(true); // Combine stdout + stderr


        try {
            Process process = pb.start();

            // Read log and print
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Real-ESRGAN ends with code : " + exitCode);

            File outputFile = new File(inputImagePath);
            if (outputFile.exists()) {
                System.out.println("✅ Image sharpening completed : " + outputFile.getAbsolutePath());
                return inputImagePath;
            } else {
                System.err.println("❌ No image found after sharpening!");
            }


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }
}
