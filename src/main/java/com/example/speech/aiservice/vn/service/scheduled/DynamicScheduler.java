package com.example.speech.aiservice.vn.service.scheduled;

import com.example.speech.aiservice.vn.service.schedule.TimeDelay;
import com.example.speech.aiservice.vn.service.workflow.novel038k.Novel038kProcessorService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Profile("Novel038k")
public class DynamicScheduler {

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final Novel038kProcessorService novel038kProcessorService;
    private final TimeDelay timeDelay;

    @Autowired
    public DynamicScheduler(Novel038kProcessorService novel038kProcessorService, TimeDelay timeDelay) {
        this.novel038kProcessorService = novel038kProcessorService;
        this.timeDelay = timeDelay;
    }

    @PostConstruct
    public void startScheduler() {
        scheduleNextRun();
    }

    private void scheduleNextRun() {
        int delay = timeDelay.getSecond();
        System.out.println("⏳ Scheduling next run after " + delay + " seconds");

        executorService.schedule(() -> {
            try {
                novel038kProcessorService.executeWorkflow();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                scheduleNextRun();
            }
        }, delay, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdownScheduler() {
        executorService.shutdownNow();
    }
}

