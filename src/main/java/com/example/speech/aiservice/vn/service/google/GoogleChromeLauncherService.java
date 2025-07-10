package com.example.speech.aiservice.vn.service.google;

import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GoogleChromeLauncherService {

    private final PropertiesService propertiesService;

    @Autowired
    public GoogleChromeLauncherService(PropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    public Process openGoogleChrome(String port, String seleniumFileName) throws IOException {
        String chromePath = propertiesService.getChromePath();
        String userDataDir = propertiesService.getUserDataDir();

        String command = chromePath + " --remote-debugging-port=" + port + " --user-data-dir=" + userDataDir + seleniumFileName + " --headless --no-sandbox --disable-gpu";
        System.out.println(command);
        return Runtime.getRuntime().exec(command);
    }

    public void shutdown(Process process) {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }
}
