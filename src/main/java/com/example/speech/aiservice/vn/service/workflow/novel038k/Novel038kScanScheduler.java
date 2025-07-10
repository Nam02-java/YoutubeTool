//package com.example.speech.aiservice.vn.service.workflow.novel038k;
//
//import com.example.speech.aiservice.vn.service.schedule.TimeDelay;
//import com.example.speech.aiservice.vn.service.wait.WaitService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Profile;
//import org.springframework.stereotype.Service;
//
//@Service
//@Profile("Novel038k") // Set profile to "Novel038k"
//public class Novel038kScanScheduler {
//
//    private final Novel038kProcessorService novel038kProcessorService;
//    private final WaitService waitService;
//    private final TimeDelay timeDelay;
//
//    @Autowired
//    public Novel038kScanScheduler(Novel038kProcessorService novel038kProcessorService, WaitService waitService, TimeDelay timeDelay) {
//        this.novel038kProcessorService = novel038kProcessorService;
//        this.waitService = waitService;
//        this.timeDelay = timeDelay;
//        startMonitoring();
//    }
//
//    public void startMonitoring() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    int second = timeDelay.getSecond();
//                    waitService.waitForSeconds(5);
//                    if (second != 0) {
//                        novel038kProcessorService.startWorkflow(second);
//                    }
//                }
//            }
//        }).start();
//    }
//}
