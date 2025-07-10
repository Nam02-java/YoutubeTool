package com.example.speech.aiservice.vn.service.google;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

@Service
public class WebDriverLauncherService {


    public WebDriver initWebDriver(String localhost) {
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", "localhost:" + localhost);
        options.addArguments("--start-maximized");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.7151.69 Safari/537.36");
     //   options.addArguments("--disable-blink-features=AutomationControlled");
       // options.addArguments("--lang=vi");
        return new ChromeDriver(options);
    }

    public void shutDown(WebDriver driver) {
        driver.quit();
    }
}
