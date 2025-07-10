package com.example.speech.aiservice.vn.service.string;

import com.example.speech.aiservice.vn.service.wait.WaitService;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TotalChapterParse {

    private final WaitService waitService;

    @Autowired
    public TotalChapterParse(WaitService waitService) {
        this.waitService = waitService;
    }

    public int parse(WebDriver driver) {

        WebElement fontElement = driver.findElement(By.cssSelector("body > main > div:nth-child(1) > div.novel > div.n-text > p:nth-child(5) > a > font > font"));
        String latestChapterText = fontElement.getText(); // Example: "Chương 1,301 Cực hạn thứ mười!"
        System.out.println("Latest chapter title: " + latestChapterText);


        int totalChapterNumber = -1;
        Pattern pattern = Pattern.compile("Chương\\s*([\\d,]+)");
        Matcher matcher = pattern.matcher(latestChapterText);

        if (matcher.find()) {
            // Plan A
            String numberStr = matcher.group(1).replace(",", "");
            totalChapterNumber = Integer.parseInt(numberStr);
            System.out.println("✅ Extracted chapter number from main text: " + totalChapterNumber);
            return totalChapterNumber;

        } else {
            // Plan B
            System.out.println("⚠️ Chapter number not found from rawText, trying to get from 'Total of ... chapters'");

            try {
                WebElement totalChapterElement = driver.findElement(By.cssSelector("body > main > div:nth-child(5) > h2 > span.sub-text-r > font > font"));
                String totalChapter = totalChapterElement.getText(); // Example: "Tổng cộng có 2048 chương"

                Pattern fallbackPattern = Pattern.compile("Tổng cộng có\\s+(\\d+)\\s+chương");
                Matcher fallbackMatcher = fallbackPattern.matcher(totalChapter);

                if (fallbackMatcher.find()) {
                    totalChapterNumber = Integer.parseInt(fallbackMatcher.group(1));
                    System.out.println("✅ Extracted chapter number from total chapter element: " + totalChapterNumber);
                    return totalChapterNumber;
                } else {
                    System.out.println("❌ Number not found in chapter total element : " + totalChapter);
                }
            } catch (NoSuchElementException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }
}
