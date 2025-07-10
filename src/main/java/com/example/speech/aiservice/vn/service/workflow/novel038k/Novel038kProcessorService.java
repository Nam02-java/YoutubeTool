package com.example.speech.aiservice.vn.service.workflow.novel038k;

import com.example.speech.aiservice.vn.dto.response.NovelInfoResponseDTO;
import com.example.speech.aiservice.vn.model.entity.chapter.Chapter;
import com.example.speech.aiservice.vn.model.entity.novel.Novel;
import com.example.speech.aiservice.vn.model.entity.selenium.SeleniumConfigSingle;
import com.example.speech.aiservice.vn.model.info.UploadTaskInfo;
import com.example.speech.aiservice.vn.service.executor.MyRunnableService;
import com.example.speech.aiservice.vn.service.filehandler.FileNameService;
import com.example.speech.aiservice.vn.service.google.GoogleChromeLauncherService;
import com.example.speech.aiservice.vn.service.google.WebDriverLauncherService;
import com.example.speech.aiservice.vn.service.image.ImageDesignService;
import com.example.speech.aiservice.vn.service.image.ImageService;
import com.example.speech.aiservice.vn.service.propertie.PropertiesService;
import com.example.speech.aiservice.vn.service.queue.ScanQueue;
import com.example.speech.aiservice.vn.service.repositoryService.chapter.ChapterService;
import com.example.speech.aiservice.vn.service.repositoryService.novel.NovelService;
import com.example.speech.aiservice.vn.service.repositoryService.selenium.SeleniumConfigSingleService;
import com.example.speech.aiservice.vn.service.schedule.TimeDelay;
import com.example.speech.aiservice.vn.service.string.ChapterLinkBuilderService;
import com.example.speech.aiservice.vn.service.string.TotalChapterParse;
import com.example.speech.aiservice.vn.service.video.VideoMergerService;
import com.example.speech.aiservice.vn.service.wait.WaitService;
import com.example.speech.aiservice.vn.service.workflow.full.FullWorkFlow;
import com.example.speech.aiservice.vn.service.youtube.YoutubeUploadService;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Novel038kProcessorService {
    private final GoogleChromeLauncherService googleChromeLauncherService;
    private final WebDriverLauncherService webDriverLauncherService;
    private final WaitService waitService;
    private final NovelService novelService;
    private final ChapterService chapterService;
    private final ExecutorService executorService;
    private final ApplicationContext applicationContext;
    private final SeleniumConfigSingleService seleniumConfigSingleService;
    private final FileNameService fileNameService;
    private volatile boolean stop = false; // Volatile variable to track STOP command - true = stopping
    private volatile String imagePath = null;
    private final TimeDelay timeDelay;
    private final PropertiesService propertiesService;
    private final ScanQueue scanQueue;
    private final ImageService imageService;
    private final ImageDesignService imageDesignService;
    private final ChapterLinkBuilderService chapterLinkBuilderService;
    private final TotalChapterParse totalChapterParse;
    private Map<String, int[]> totalVideoMap = new LinkedHashMap<>();
    private final NavigableMap<Integer, String> videoPathMap = new ConcurrentSkipListMap<>();
    private final VideoMergerService videoMergerService;
    private final YoutubeUploadService youtubeUploadService;
    private final Map<String, String> chapterMap038K;

    private final List<String> novelLinks = Arrays.asList(
            "http://www.038k.com/xs/93/93809/",
            "http://www.038k.com/xs/97/97006/",
            "http://www.038k.com/xs/72/72136/",
            "http://www.038k.com/xs/17/17456/"

    );
    private int currentIndex = 0;

    private final Map<String, Queue<UploadTaskInfo>> uploadQueueMap = new ConcurrentHashMap<>();


    @Autowired
    public Novel038kProcessorService(
            GoogleChromeLauncherService googleChromeLauncherService,
            WebDriverLauncherService webDriverLauncherService,
            WaitService waitService, NovelService novelService,
            ChapterService chapterService,
            ApplicationContext applicationContext,
            SeleniumConfigSingleService seleniumConfigSingleService,
            FileNameService fileNameService,
            TimeDelay timeDelay,
            PropertiesService propertiesService,
            ScanQueue scanQueue,
            ImageService imageService,
            ImageDesignService imageDesignService,
            ChapterLinkBuilderService chapterLinkBuilderService,
            TotalChapterParse totalChapterParse,
            VideoMergerService videoMergerService,
            YoutubeUploadService youtubeUploadService) {
        this.googleChromeLauncherService = googleChromeLauncherService;
        this.webDriverLauncherService = webDriverLauncherService;
        this.waitService = waitService;
        this.novelService = novelService;
        this.chapterService = chapterService;
        this.applicationContext = applicationContext;
        this.seleniumConfigSingleService = seleniumConfigSingleService;
        this.fileNameService = fileNameService;
        this.timeDelay = timeDelay;
        this.propertiesService = propertiesService;
        this.scanQueue = scanQueue;
        this.imageService = imageService;
        this.imageDesignService = imageDesignService;
        this.chapterLinkBuilderService = chapterLinkBuilderService;
        this.totalChapterParse = totalChapterParse;
        this.videoMergerService = videoMergerService;
        this.youtubeUploadService = youtubeUploadService;
        this.chapterMap038K = new LinkedHashMap<>();
        this.executorService = Executors.newFixedThreadPool(3);
    }

    public void executeWorkflow() {

        List<SeleniumConfigSingle> threadConfigs = seleniumConfigSingleService.getAllConfigs();

        SeleniumConfigSingle defaultSeleniumConfigSingle = threadConfigs.get(4); // PORT : 2226

        WebDriver driver = null;
        Process chromeProcess = null;
        NovelInfoResponseDTO novelInfo = null;

        String inputLink = null;

        Boolean flag = false;

        int totalChapterNumber = 0;
        try {

            /**
             * input link from commandlistenerapp
             */

            //inputLink = scanQueue.takeFromQueue();
            //inputLink = "http://www.038k.com/xs/72/72136/";

            inputLink = novelLinks.get(currentIndex);
            currentIndex = (currentIndex + 1) % novelLinks.size();
            System.out.println("\uD83C\uDCCF Current inputLink : " + inputLink);

            chromeProcess = googleChromeLauncherService.openGoogleChrome(defaultSeleniumConfigSingle.getPort(), defaultSeleniumConfigSingle.getSeleniumFileName());
            try {
                driver = webDriverLauncherService.initWebDriver(defaultSeleniumConfigSingle.getPort());
            } catch (Exception e) {
                e.printStackTrace();
            }

            novelInfo = scanNovelTitle(driver, inputLink, chapterMap038K);

            safeNovelToDatabase(novelInfo);

            totalChapterNumber = scanTotalChapterNumber(driver);

            int totalChapterPerVideo = Integer.parseInt(propertiesService.getTotalChapterPerVideo());

            totalVideoMap = getVideoChapterMap(novelInfo, totalChapterNumber, totalChapterPerVideo);
            for (Map.Entry<String, int[]> entry : totalVideoMap.entrySet()) {
                String key = entry.getKey();
                int[] value = entry.getValue();
                System.out.println("Key: " + key + ", Value: " + Arrays.toString(value));
            }

            imagePath = imageService.getValidImagePath(driver, chromeProcess, inputLink, googleChromeLauncherService, webDriverLauncherService, defaultSeleniumConfigSingle.getPort(), defaultSeleniumConfigSingle.getSeleniumFileName(), novelInfo.getTitle());
            //imagePath = editPictureNow(totalVideoMap, title);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            webDriverLauncherService.shutDown(driver);
            googleChromeLauncherService.shutdown(chromeProcess);
        }

        if (!stop) {

            System.out.println("Stop is false");

            Novel novel = novelService.findByTitle(novelInfo.getTitle());
            Long novelId = novel.getId();

            int maxChapterNumber;
            Optional<Chapter> lastChapter = chapterService.findLastScannedChapter(novelId);
            if (lastChapter.isPresent()) {
                maxChapterNumber = lastChapter.get().getChapterNumber();
                System.out.println("\n" + "\uD83D\uDD0A Last scanned chapter: " + maxChapterNumber);
            } else {
                maxChapterNumber = 0;
                System.out.println("No chapters scanned yet");
            }

            while (true) {

                if (stop) {
                    System.out.println("STOP command received! No new tasks will be started.");
                    timeDelay.setSecond(5);
                    return;
                }

                if (maxChapterNumber == totalChapterNumber) {
                    System.out.println("The entire story has been scanned !");
                    timeDelay.setSecond(5);
                    return;
                }

                if (flag) {
                    System.out.println("\uD83C\uDF93 Produce a successful video, check the next story in the map!");
                    timeDelay.setSecond(5);
                    return;
                }


                int maxThreads = calculateThreadCount(totalVideoMap, maxChapterNumber);
                if (maxThreads == 0) {
                    System.out.println("✅ All chapters scanned.");
                    return;
                }

                CountDownLatch latch = new CountDownLatch(maxThreads);
                boolean stop = false;
                String chapterLinkToScan = null;

                for (int i = 0; i < maxThreads; i++) {
                    SeleniumConfigSingle config = threadConfigs.get(i);

                    FullWorkFlow fullWorkFlow = applicationContext.getBean(FullWorkFlow.class);

                    /**
                     * For example: no chapter has been scanned yet, after each for loop +=1, the total scan is 3 -> continue
                     */
                    maxChapterNumber += 1;

                    if (maxChapterNumber > totalChapterNumber) {
                        latch.countDown();
                        break;
                    }

                    boolean shouldBreakOuterLoop = false;

                    if (!totalVideoMap.isEmpty()) {
                        // Compare with videoMap entries
                        for (Map.Entry<String, int[]> entryTotalVideoMap : totalVideoMap.entrySet()) {
                            int[] range = entryTotalVideoMap.getValue();
                            int end = range[1];
                            if (maxChapterNumber > end) {
                                maxChapterNumber = maxChapterNumber - 1;
                                shouldBreakOuterLoop = true;
                                break;
                            }
                        }
                    }

                    if (shouldBreakOuterLoop) {
                        latch.countDown();
                        continue;
                    }

                    //  String chapterLinkToScan = chapterLinkBuilderService.buildChapterLink(novelInfo.getLink(), maxChapterNumber);

                    // Get last key in chapterMap038K
                    if (!chapterMap038K.isEmpty()) {
                        String lastKey = null;
                        for (String key : chapterMap038K.keySet()) {
                            lastKey = key;
                        }
                        chapterLinkToScan = chapterMap038K.get(lastKey);
                        chapterMap038K.remove(lastKey);
                    } else {

                        if (!stop) {
                            Optional<Chapter> latestChapterOpt = chapterService.getLatestChapterByNovelLink(inputLink);
                            if (latestChapterOpt.isPresent()) {
                                stop = true;
                                chapterLinkToScan = latestChapterOpt.get().getLink();
                            }
                        }

                        try {
                            chromeProcess = googleChromeLauncherService.openGoogleChrome(defaultSeleniumConfigSingle.getPort(), defaultSeleniumConfigSingle.getSeleniumFileName());
                            driver = webDriverLauncherService.initWebDriver(defaultSeleniumConfigSingle.getPort());

                            driver.get(chapterLinkToScan);

                            waitService.waitForSeconds(1);

                            // Scroll to the bottom of the page
                            JavascriptExecutor js = (JavascriptExecutor) driver;
                            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");

                            waitService.waitForSeconds(1);

                            try {
                                // Locate the "Next Chapter" button based on text content
                                WebElement nextChapterButton = driver.findElement(
                                        By.cssSelector("#container > div > div > div.reader-main > div.section-opt.m-bottom-opt > a:nth-child(6)")
                                );

                                // Click the button
                                nextChapterButton.click();
                                System.out.println("✅ Clicked the 'Next Chapter' button successfully.");

                                waitService.waitForSeconds(1);

                                chapterLinkToScan = driver.getCurrentUrl();

                            } catch (NoSuchElementException e) {
                                System.err.println("❌ 'Next Chapter' button not found on the page.");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            webDriverLauncherService.shutDown(driver);
                            googleChromeLauncherService.shutdown(chromeProcess);
                        }
                    }


                    System.out.println("\uD83D\uDCBB Scan URL : " + chapterLinkToScan);

                    MyRunnableService myRunnableService = new MyRunnableService(
                            fullWorkFlow,
                            config.getPort(), config.getSeleniumFileName(),
                            novel, chapterLinkToScan,
                            imagePath,
                            videoPathMap, totalVideoMap,
                            maxChapterNumber);

                    executorService.execute(() -> {
                        try {
                            myRunnableService.run();
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    System.err.println("Error completing task : " + e.getMessage());
                }

                // Compare with first video
                String uploadVideoDirectoryPath = null;
                String novelTitle = novel.getTitle();
                int start = 0;
                int end = 0;

                if (!totalVideoMap.isEmpty()) {

                    // Compare with videoMap entries
                    for (Map.Entry<String, int[]> entryTotalVideoMap : totalVideoMap.entrySet()) {
                        int[] range = entryTotalVideoMap.getValue();
                        start = range[0];
                        end = range[1];

                        if (maxChapterNumber >= end) { // ==
                            if (videoPathMap.size() != end) {
                                if (!videoPathMap.isEmpty()) {
                                    Map.Entry<Integer, String> firstEntry = videoPathMap.firstEntry();
                                    if (firstEntry != null) {
                                        String firstVideoPath = firstEntry.getValue();
                                        uploadVideoDirectoryPath = new File(firstVideoPath).getParent();

                                        File folder = new File(uploadVideoDirectoryPath);
                                        File[] allFiles = folder.listFiles((dir, name) -> name.endsWith(".mp4"));

                                        if (allFiles != null) {
                                            for (int chapterNumber = start; chapterNumber <= end; chapterNumber++) {
                                                if (videoPathMap.containsKey(chapterNumber)) {
                                                    continue; // already have then ignore
                                                }

                                                // Find files starting with chapter number
                                                for (File file : allFiles) {
                                                    String fileName = file.getName();
                                                    if (fileName.startsWith(chapterNumber + ".")) {
                                                        videoPathMap.put(chapterNumber, file.getAbsolutePath());
                                                        break; // only get the first matching file
                                                    }
                                                }

                                                // If still not found
                                                if (!videoPathMap.containsKey(chapterNumber)) {
                                                    System.err.println("❌ File not found for chapter " + chapterNumber);
                                                }
                                            }
                                        } else {
                                            System.err.println("❌ Cannot read video folder : " + uploadVideoDirectoryPath);
                                        }
                                    }
                                } else {
                                    System.err.println("❌ videoPathMap is empty, cannot deduce directory containing video");
                                }
                            }

                            System.out.println("📄 List of collected chapter videos:");
                            System.out.println("────────────────────────────────────────────");
                            for (Map.Entry<Integer, String> entry : videoPathMap.entrySet()) {
                                int chapter = entry.getKey();
                                String path = entry.getValue();
                                System.out.printf("Chapter %d → %s%n", chapter, path);
                            }
                            System.out.println("────────────────────────────────────────────");
                            System.out.println("✅ Total chapters collected: " + videoPathMap.size());


                            // Get only videos within chapter range [start, end]
                            List<String> sortedVideoPaths = new ArrayList<>();
                            for (Map.Entry<Integer, String> entryVideoPathMap : videoPathMap.entrySet()) {
                                int chapter = entryVideoPathMap.getKey();
                                if (chapter >= start && chapter <= end) {
                                    sortedVideoPaths.add(entryVideoPathMap.getValue());
                                }
                            }

                            String videoName = entryTotalVideoMap.getKey();
                            try {
                                uploadVideoDirectoryPath = videoMergerService.mergeVideos(sortedVideoPaths, novel, videoName);

                                /**
                                 * 🎓 Produce a successful video, check the next story in the map!
                                 */
                                flag = true;

                            } catch (IOException | InterruptedException e) {
                                throw new RuntimeException(e);
                            }

                            // Delete merged path from map
                            for (int i = start; i <= end; i++) {
                                videoPathMap.remove(i);
                            }

                            totalVideoMap.remove(videoName);
                            System.out.println("🎯 Completed " + videoName);


                            String description = novelInfo.getStoryTitle();
                            String playListName = novelTitle;
                            String rangeChapter = "Chương " + start + "-" + end;

                            UploadTaskInfo taskInfo = new UploadTaskInfo(
                                    uploadVideoDirectoryPath,
                                    videoName,
                                    description,
                                    playListName,
                                    imagePath,
                                    rangeChapter
                            );

//                            uploadQueueMap
//                                    .computeIfAbsent(inputLink, k -> new LinkedList<>())
//                                    .add(taskInfo);

                            waitService.waitForSeconds(5);
                            youtubeUploadService.upload(
                                    inputLink,
                                    novel,
                                    taskInfo.getVideoPath(),
                                    taskInfo.getTitle(),
                                    taskInfo.getDescription(),
                                    taskInfo.getPlayListName(),
                                    imagePath,
                                    taskInfo.getRangeChapter()
                            );
                            waitService.waitForSeconds(5);


                            /**
                             * delete current image path
                             */
                            // deleteOldImagePath(imagePath);

                            /**
                             * change image path of range
                             */
                            // imagePath = nextImagePath(totalVideoMap, novelTitle);
                            break;
                        }
                    }
                } else {
                    System.out.println("video map is null");
                }

//                int hour = LocalTime.now().getHour();
//
//                for (Map.Entry<String, Queue<UploadTaskInfo>> entry : uploadQueueMap.entrySet()) {
//                    String key = entry.getKey();
//                    Queue<UploadTaskInfo> uploadQueue = entry.getValue();
//
//                    boolean isUploadTime = false;
//
//                    if (key.contains("72136") && hour % 2 == 0) { // phong thủy dân gian
//                        isUploadTime = true;
//                    } else if (key.contains("17456") && hour == 18) { // thu gom xác chết
//                        isUploadTime = true;
//                    }
//
//                    if (!isUploadTime) {
//                        System.out.println("⏳ Not upload time for key: " + key + " (hour: " + hour + ")");
//                        continue;
//                    }
//
//                    while (!uploadQueue.isEmpty()) {
//                        UploadTaskInfo uploadTaskInfo = uploadQueue.poll(); // remove and get
//
//                        String uploadVideoPath = uploadTaskInfo.getVideoPath();
//                        String videoName = uploadTaskInfo.getTitle();
//                        String description = uploadTaskInfo.getDescription();
//                        String playListName = uploadTaskInfo.getPlayListName();
//                        String imagePath = uploadTaskInfo.getImagePath();
//                        String rangeChapter = uploadTaskInfo.getRangeChapter();
//
//                        waitService.waitForSeconds(5);
//                        youtubeUploadService.upload(
//                                novel,
//                                uploadVideoPath,
//                                videoName,
//                                description,
//                                playListName,
//                                imagePath,
//                                rangeChapter
//                        );
//                        waitService.waitForSeconds(5);
//                    }
                //              }
                System.out.println("\uD83C\uDF04 Complete threads, continue scanning...");
            }


        } else {
            System.out.println("stop is true");
            stopConditions();
            timeDelay.setSecond(5000);
        }
    }

    public int calculateThreadCount(Map<String, int[]> chapterMap, int maxChapterNumber) {
        for (Map.Entry<String, int[]> entry : chapterMap.entrySet()) {
            int[] range = entry.getValue(); // Ví dụ: [2098, 2101]
            if (range.length != 2) continue;

            int chapterEnd = range[1];
            int gap = chapterEnd - maxChapterNumber;

            if (gap <= 0) {
                return 0; // đã quét hết
            } else {
                return Math.min(gap, 3); // còn 1–2 chương thì dùng 1–2 thread
            }
        }
        return 0; // nếu map rỗng
    }


    private String editPictureNow(Map<String, int[]> totalVideoMap, String safeTitle) {

        for (Map.Entry<String, int[]> entryTotalVideoMap : totalVideoMap.entrySet()) {
            int[] range = entryTotalVideoMap.getValue();
            int start = range[0];
            int end = range[1];
            System.out.println("\uD83D\uDE93 All range : " + start + " - " + end);
        }

        System.out.println();
        for (Map.Entry<String, int[]> entryTotalVideoMap : totalVideoMap.entrySet()) {
            int[] range = entryTotalVideoMap.getValue();
            int start = range[0];
            int end = range[1];
            System.out.println("\uD83D\uDE93 The range at this point is : " + start + " - " + end);
            break;
        }

        String input = null;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter ok when you edit done yet : ");
        while (true) {
            input = scanner.nextLine();
            if (input.equals("ok")) {
                String imageDirectoryPath = propertiesService.getImageDirectory();
                String safeNovelTitle = fileNameService.sanitizeFileName(safeTitle);
                String imageDirectory = imageDirectoryPath + File.separator + safeNovelTitle;
                fileNameService.ensureDirectoryExists(imageDirectory);

                File dir = new File(imageDirectory);
                String firstPngPath = null;

                if (dir.exists() && dir.isDirectory()) {
                    File[] pngFiles = dir.listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.toLowerCase().endsWith(".png");
                        }
                    });

                    if (pngFiles != null && pngFiles.length > 0) {
                        firstPngPath = pngFiles[0].getAbsolutePath();
                        System.out.println("✅ First image in file : " + firstPngPath);
                        return firstPngPath.toString();
                    }
                }
                break;
            }
        }
        return null;
    }

    private String nextImagePath(Map<String, int[]> totalVideoMap, String safeTitle) {

        String imageDirectoryPath = propertiesService.getImageDirectory();
        String safeNovelTitle = fileNameService.sanitizeFileName(safeTitle);
        String imageDirectory = imageDirectoryPath + File.separator + safeNovelTitle;
        fileNameService.ensureDirectoryExists(imageDirectory);

        File dir = new File(imageDirectory);
        String firstPngPath = null;

        if (dir.exists() && dir.isDirectory()) {
            File[] pngFiles = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".png");
                }
            });

            if (pngFiles != null && pngFiles.length > 0) {
                firstPngPath = pngFiles[0].getAbsolutePath();
                System.out.println("✅ First image in file : " + firstPngPath);
                return firstPngPath.toString();
            }
        }


        for (Map.Entry<String, int[]> entryTotalVideoMap : totalVideoMap.entrySet()) {
            int[] range = entryTotalVideoMap.getValue();
            int start = range[0];
            int end = range[1];
            System.out.println("\uD83D\uDE93 All range : " + start + " - " + end);
        }

        System.out.println();
        for (Map.Entry<String, int[]> entryTotalVideoMap : totalVideoMap.entrySet()) {
            int[] range = entryTotalVideoMap.getValue();
            int start = range[0];
            int end = range[1];
            System.out.println("\uD83D\uDE93 The range at this point is : " + start + " - " + end);
            break;
        }
        String input = null;
        Scanner scanner = new Scanner(System.in);
        System.out.println("\uD83E\uDD20 Enter ok when you have new picture because you don't have a picture now ! : ");
        while (true) {
            input = scanner.nextLine();
            if (input.equals("ok")) {
                break;
            }
        }
        File[] updatedPngFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".png");
            }
        });

        if (updatedPngFiles != null && updatedPngFiles.length > 0) {
            String newFirstImage = updatedPngFiles[0].getAbsolutePath();
            System.out.println("✅ New image selected after ok: " + newFirstImage);
            return newFirstImage;
        } else {
            System.out.println("❌ No PNG file found after ok.");
            return null;
        }
    }

    private void deleteOldImagePath(String imagePath) {
        System.out.println("\uD83D\uDC4A Delete this shit : " + imagePath);
        new File(imagePath).delete();
    }

    private void safeNovelToDatabase(NovelInfoResponseDTO novelInfo) {
        Novel novel = new Novel(novelInfo.getTitle(), novelInfo.getLink());
        if (!novelService.isNovelExistsByLink(novel.getLink())) {
            novelService.saveNovel(novel);
        } else {
            System.out.println("\uD83D\uDC80 " + novelInfo.getTitle() + " already exists in the database");
        }
    }

    private Map<String, int[]> getVideoChapterMap(NovelInfoResponseDTO novelInfoResponseDTO,
                                                  int totalChapters,
                                                  int chaptersPerVideo) {
        Map<String, int[]> map = new LinkedHashMap<>();

        Novel novel = novelService.findByTitle(novelInfoResponseDTO.getTitle());

        int lastScannedChapter = chapterService.findLastScannedChapter(novel.getId())
                .map(Chapter::getChapterNumber)
                .orElse(0);

        String title = novelInfoResponseDTO.getTitle();

        int start = lastScannedChapter + 1;

        while (start <= totalChapters) {
            int end = Math.min(start + chaptersPerVideo - 1, totalChapters);

            String videoName = title + " || Chương " + start + " - " + end;
            map.put(videoName, new int[]{start, end});

            start = end + 1;
        }

        return map;
    }

//    private Map<String, int[]> getVideoChapterMap(NovelInfoResponseDTO novelInfoResponseDTO, int totalChapters,
//                                                  int chaptersPerVideo) {
//        Map<String, int[]> map = new LinkedHashMap<>(); // new local map
//
//        int totalVideos = (int) Math.ceil((double) totalChapters / chaptersPerVideo);
//
//        String title = novelInfoResponseDTO.getTitle();
//        for (int i = 0; i < totalVideos; i++) {
//            int start = i * chaptersPerVideo + 1;
//            int end = Math.min((i + 1) * chaptersPerVideo, totalChapters);
//
//            Novel novel = novelService.findByTitle(novelInfoResponseDTO.getTitle());
//            boolean chapter = chapterService.isExistsByNovelAndChapterNumber(novel, end);
//            if (chapter) {
//                continue;
//            }
//
//            String videoName = title + " || Chương " + start + " - " + end;
//            map.put(videoName, new int[]{start, end});
//        }
//
//        return map;
//    }


    private NovelInfoResponseDTO scanNovelTitle(WebDriver driver, String inputLink, Map<String, String> chapterMap038K) {
        scanQueue.printQueue();
        try {

            driver.get(inputLink);

            /**
             * wait for translate
             */
            //waitService.waitForSeconds(3);
            waitService.waitForSeconds(3);
            driver.navigate().refresh();
            waitService.waitForSeconds(3);

            String homePage038kURL = propertiesService.getHomePage038kURL();

            Optional<Chapter> latestChapterOpt = chapterService.getLatestChapterByNovelLink(inputLink);
            if (latestChapterOpt.isPresent()) {

            } else {

                if (inputLink.contains(homePage038kURL)) {
                    List<WebElement> sectionBoxes = driver.findElements(By.cssSelector(".section-box .section-list"));

                    if (!sectionBoxes.isEmpty()) {
                        WebElement correctSection = sectionBoxes.get(1);

                        List<WebElement> chapterLinks = correctSection.findElements(By.tagName("a"));

                        int numberChapterParseToInt = 0;

                        for (WebElement chapterLink : chapterLinks) {
                            String href = chapterLink.getAttribute("href");
                            numberChapterParseToInt++;

                            String fullLink = href.startsWith("http") ? href : homePage038kURL + href;
                            String numberChapterParseToString = String.valueOf(numberChapterParseToInt);

                            chapterMap038K.put(numberChapterParseToString, fullLink);

                            break; // Only get first chapter
                        }
                    }
                    System.out.println("✅ Chapter map: " + chapterMap038K);
                }
            }

            // Get novel title
//            WebElement element = driver.findElement(By.cssSelector(
//                    "body > div.container > div.row.row-detail > div > div > div.info > div.top > h1 > font > font"));
            // String title = element.getText().trim();
            String title = null;
            //  if (title.isBlank() || title.isEmpty()) {
            System.out.println("bro is null T_T");
            if (inputLink.contains("72136")) { // Bí Quyết Phong Thủy Dân Gian
                title = "Bí Quyết Phong Thủy Dân Gian";
            } else if (inputLink.contains("17456")) { // Người thu gom xác chết
                title = "Người thu gom xác chết";
            } else if (inputLink.contains("97006")) {
                title = "Vào cuối thời nhà Minh, xây dựng lại thế giới từ phía tây bắc";
            } else if (inputLink.contains("93809")) {
                title = "Tôi và cô hàng xóm bị mắc kẹt trên một hòn đảo hoang";
                //      }
            }
            String safeTitle = title.split(" - ", 2)[0].trim();
            System.out.println("Title : " + safeTitle);

            System.out.println("Enter your YouTube description (type 'ok' on a new line to finish):");
//                Scanner scanner = new Scanner(System.in);
//                while (true) {
//                    String line = scanner.nextLine();
//                    if (line.equalsIgnoreCase("ok")) break;
//                    descriptionBuilder.append(line).append(System.lineSeparator());
//                }a
            //  String storyTitle = descriptionBuilder.toString().trim();

            StringBuilder descriptionBuilder = new StringBuilder();

            if (inputLink.contains("72136")) { // Bí Quyết Phong Thủy Dân Gian
                descriptionBuilder.append("#truyenma #truyenhay #truyệnkinhdi\n");
                descriptionBuilder.append("🎧 Truyện Audio: Bí Thuật Phong Thủy Dân Gian - Thực Lục Phong Thủy 🎧\n\n");
                descriptionBuilder.append("📜 Thực Lục Phong Thủy 📜\n\n");
                descriptionBuilder.append("Giữa màn sương dày đặc của rừng sâu phương Nam, tồn tại một truyền thuyết cổ xưa về “Thực Lục Phong Thủy” – cuốn bí thư ghi chép những quy tắc thâm sâu của trời đất, âm dương, long mạch và quỷ dị.\n\n");
                descriptionBuilder.append("Lý Thừa Phong – một chàng thanh niên có ngoại hình tuấn tú, sinh ra trong dòng tộc trấn trạch nổi danh – tình cờ phát hiện một chiếc la bàn cổ khắc trận đồ Thái Cực, mở ra cánh cửa kết nối nhân – quỷ – thần.\n\n");
                descriptionBuilder.append("Mang trong mình thiên phú dị bẩm và chí nguyện phá giải các thế trận oán khí, anh bắt đầu hành trình chu du bốn phương: trấn yểm cổ trấn, giải trừ huyết tế, đối đầu tà thuật, và hé mở những bí ẩn đằng sau những cái chết ly kỳ.\n\n");
                descriptionBuilder.append("Tuy nhiên, càng tiến sâu vào chân lý phong thủy, Lý Thừa Phong càng nhận ra mình chỉ là một quân cờ trong đại cục đã được sắp đặt từ ngàn năm trước...\n\n");
                descriptionBuilder.append("#truyenma #truyenhay #truyệnkinhdi #thuclucphongthuy #bithuatphongthuydangian ");
                descriptionBuilder.append("#lythuaphong #lýthừaphong #truyendai #chuyentamlinh #radiochuyenla ");
                descriptionBuilder.append("#truyenkhampha #truyengiaitri #truyendoc2025 #chuyenlamoingay2210");
            } else if (inputLink.contains("17456")) { // Người thu gom xác chết
                descriptionBuilder.append("#truyenma #truyenhay #truyệnkinhdi #chuyentamlinh #radiochuyenla\n");
                descriptionBuilder.append("🎧 Truyện Audio: Người Thu Gom Xác Chết 🎧\n\n");
                descriptionBuilder.append("🕯 *Người Thu Gom Xác Chết* là câu chuyện đầy ám ảnh về Giang Hạ, một thanh niên ưu tú, tài năng và mang trong mình nhiều lý tưởng sống lớn.\n\n");
                descriptionBuilder.append("Nhưng thay vì bước đi giữa ánh sáng, anh lại sống giữa bóng tối, đối mặt với những cái chết, tổ chức bí mật và những lời khuyên từ ba người anh – tất cả đều muốn kéo anh ra khỏi con đường đó.\n\n");
                descriptionBuilder.append("Kỳ lạ thay, càng khuyên nhủ, càng khiến người ta đặt câu hỏi: *rốt cuộc, Giang Hạ là ai? Và tại sao anh phải thay đổi?*\n\n");
                descriptionBuilder.append("Một bộ truyện mang màu sắc tâm linh – tâm lý – điều tra, nơi sự thật và dối trá lẫn lộn trong màn đêm.\n\n");
                descriptionBuilder.append("#nguoithugomxacchet #giangha #truyendai #truyendoc2025 #truyenamanh #truyenkhampha #chuyenlamoingay2210");
            } else if (inputLink.contains("97006")) { // Vào cuối thời nhà Minh
                descriptionBuilder.append("#truyencochien #truyenlichsu #truyenxuyenkhong #truyenhay #truyendai\n");
                descriptionBuilder.append("🎧 Truyện Audio: Vào Cuối Thời Nhà Minh - Xây Dựng Lại Thế Giới Từ Phía Tây Bắc 🎧\n\n");
                descriptionBuilder.append("⚔ *Vào Cuối Thời Nhà Minh* là bản hùng ca đẫm máu giữa thời kỳ rối ren nhất của lịch sử Trung Hoa.\n\n");
                descriptionBuilder.append("Năm Sùng Trinh thứ nhất, phía Bắc Thiểm Tây chìm trong đại hạn. Đất khô cằn, mùa màng thất bát, người chết đói khắp nơi. Liên tiếp các năm sau, hạn hán nối tiếp, cái đói lan tràn như bóng ma ám ảnh khắp châu quận.\n\n");
                descriptionBuilder.append("Trong khung cảnh đen tối ấy, một người du hành thời gian xuất hiện giữa vùng đất hoang tàn – nơi người sống thoi thóp, người chết không ai chôn. Anh cất tiếng: *“Một bát cháo đổi lấy sinh mệnh, theo ta – lật đổ thế giới thối nát này!”*\n\n");
                descriptionBuilder.append("Từ đó, ngọn lửa nổi dậy bùng cháy dữ dội tại Tây Bắc. Dưới ngọn cờ Đại Thuận, những người nghèo khổ lần lượt tụ họp, trở thành đội quân khát máu quyết tâm vùi chôn triều đại đã mục nát. \n\n");
                descriptionBuilder.append("\uD83D\uDD25 Một bộ truyện xuyên không – quân sự – lịch sử, nơi sự sống và cái chết chỉ cách nhau một bát cháo, nơi kẻ yếu đứng lên làm chủ vận mệnh.\n\n");
                descriptionBuilder.append("#chuyenlamoingay2210 #truyencochien #truyenlichsu #truyenxuyenkhong #truyenhay #truyendai\n");
                descriptionBuilder.append("#truyencochien #truyenxuyenkhong #dailuanloctroi #truyendai #truyendoc2025 #daisuhaichieu #truyenlichsu #truyenkichtinh #chientranhlsg #daithuan ");
            } else if (inputLink.contains("93809")) { // Tôi và cô hàng xóm bị mắc kẹt trên một hòn đảo hoang
                descriptionBuilder.append("#chuyenlamoingay2210 #truyenhuyenbi #truyensinhton #truyenhaihuoc #truyennguoilon #truyenlightnovel #truyenhay\n");
                descriptionBuilder.append("🏝️ Truyện Audio: Tôi và cô hàng xóm bị mắc kẹt trên một hòn đảo hoang 🏝️\n\n");
                descriptionBuilder.append("Một tia sét kỳ lạ đã đánh trúng du thuyền du lịch.\n");
                descriptionBuilder.append("Chu Phong tỉnh dậy một mình trên bãi biển… và bên cạnh cậu là cô hàng xóm Tần Tiểu Tuyết – người phụ nữ quyến rũ mà bao thanh niên thầm mơ tưởng.\n\n");
                descriptionBuilder.append("Không có hệ thống, không có hack cheat, chỉ có… bản năng sinh tồn.\n");
                descriptionBuilder.append("Từ việc nhóm lửa, tìm nước, đến dựng chỗ trú ẩn, cả hai phải đối mặt với bí ẩn rùng rợn ẩn sâu trong hòn đảo tưởng như hoang vu này.\n\n");
                descriptionBuilder.append("🔥 Giữa thiên nhiên hoang dã, sự cách biệt tuổi tác và thân phận chỉ khiến ngọn lửa khao khát thêm mãnh liệt...\n");
                descriptionBuilder.append("Cùng khám phá câu chuyện sinh tồn ly kỳ, đầy cảm xúc – nơi bản năng, lòng tin và những bí mật không tưởng đan xen từng ngày!\n\n");
                descriptionBuilder.append("#sinhton #truyendainhan #truyen18plus #truyenhaihuoc #truyennguoilon #truyenthamhiem\n");
            }


            String storyTitle = descriptionBuilder.toString();

            System.out.println("\n📄 YouTube Description:");
            System.out.println(storyTitle);

            return new NovelInfoResponseDTO(safeTitle, storyTitle, inputLink);


        } catch (
                Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private int scanTotalChapterNumber(WebDriver driver) {
        WebElement fontElement = driver.findElement(By.cssSelector(
                "body > div.container > div.row.row-detail > div > div > div.info > div.top > div > p:nth-child(6) > a > font > font"));
        String latestChapterText = fontElement.getText();
        System.out.println("📄 Raw chapter text: " + latestChapterText);

        int totalChapterNumber = -1;

        // Plan A: Check for pattern "Chương xxx"
        Pattern patternChuong = Pattern.compile("Chương\\s*([\\d,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = patternChuong.matcher(latestChapterText);
        if (matcher.find()) {
            String numberStr = matcher.group(1).replace(",", "");
            totalChapterNumber = Integer.parseInt(numberStr);
            System.out.println("✅ Plan A: Matched 'Chương xxx' pattern: " + totalChapterNumber);
        }

        // Plan B: Check if string starts with a number (e.g. "3271 ...")
        else {
            Pattern patternStartNumber = Pattern.compile("^(\\d{1,5})");
            matcher = patternStartNumber.matcher(latestChapterText);
            if (matcher.find()) {
                String numberStr = matcher.group(1);
                totalChapterNumber = Integer.parseInt(numberStr);
                System.out.println("✅ Plan B: Matched number at start of string: " + totalChapterNumber);
            }
            // Plan D: Check for pattern number followed by [Theo dõi từ FBI]
            else if (totalChapterNumber == -1) {
                Pattern patternTheoDoi = Pattern.compile("^(\\d{1,5})\\s*\\[Theo dõi từ FBI\\]");
                Matcher matcherTheoDoi = patternTheoDoi.matcher(latestChapterText);
                if (matcherTheoDoi.find()) {
                    String numberStr = matcherTheoDoi.group(1);
                    totalChapterNumber = Integer.parseInt(numberStr);
                    System.out.println("✅ Plan D: Matched pattern number + [Theo dõi từ FBI]: " + totalChapterNumber);
                }
                // Plan C: Fallback to "Tổng cộng có xxx chương"
                else {
                    System.out.println("⚠️ Plan A, B & D failed. Trying Plan C: fallback total chapter element.");
                    try {
                        WebElement totalChapterElement = driver.findElement(By.cssSelector(
                                "body > main > div:nth-child(5) > h2 > span.sub-text-r > font > font"));
                        String totalChapterText = totalChapterElement.getText();
                        System.out.println("📄 Fallback text: " + totalChapterText);

                        Pattern patternTotal = Pattern.compile("Tổng cộng có\\s+(\\d+)\\s+chương");
                        Matcher fallbackMatcher = patternTotal.matcher(totalChapterText);
                        if (fallbackMatcher.find()) {
                            totalChapterNumber = Integer.parseInt(fallbackMatcher.group(1));
                            System.out.println("✅ Plan C: Matched fallback total chapter number: " + totalChapterNumber);
                        } else {
                            System.out.println("❌ Plan C failed: No number found in fallback string.");
                        }
                    } catch (NoSuchElementException e) {
                        System.out.println("❌ Plan C failed: Fallback element not found.");
                        e.printStackTrace();
                    }
                }
            }
        }

        if (totalChapterNumber == -1) {
            System.out.println("❌ All extraction plans failed. Returning 0.");
            return 0;
        }

        return totalChapterNumber;
    }


    private int getMaxChapterNumber() {
        int maxChapter = 0;
        for (String chapterStr : chapterMap038K.keySet()) {
            try {
                int chapterNum = Integer.parseInt(chapterStr);
                if (chapterNum > maxChapter) {
                    maxChapter = chapterNum;
                }
            } catch (NumberFormatException e) {
            }
        }
        return maxChapter;
    }

    public void stopConditions() {
        imagePath = null;
        stop = true;
        videoPathMap.clear();
        totalVideoMap.clear();
    }
}
