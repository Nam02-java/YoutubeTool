package com.example.speech.aiservice.vn.service.workflow.ixdzs8;

import com.example.speech.aiservice.vn.service.schedule.TimeDelay;
import com.example.speech.aiservice.vn.service.wait.WaitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("Ixdzs8") // Set profile to "Ixdzs8"
public class Ixdzs8ScanScheduler {
    private final Ixdzs8ProcessorService singleNovelPreProcessorService;
    private final WaitService waitService;
    private final TimeDelay timeDelay;

    @Autowired
    public Ixdzs8ScanScheduler(Ixdzs8ProcessorService singleNovelPreProcessorService, WaitService waitService, TimeDelay timeDelay) {
        this.singleNovelPreProcessorService = singleNovelPreProcessorService;
        this.waitService = waitService;
        this.timeDelay = timeDelay;
        startMonitoring();
    }

    public void startMonitoring() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    int second = timeDelay.getSecond();
                    waitService.waitForSeconds(5);
                    if (second != 0) {
                        singleNovelPreProcessorService.startWorkflow(second);
                    }
                }
            }
        }).start();
    }
}
