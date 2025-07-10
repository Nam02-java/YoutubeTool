package com.example.speech.aiservice.vn.controller;

import com.example.speech.aiservice.vn.dto.request.WorkFlowScanRequestDTO;
import com.example.speech.aiservice.vn.service.queue.ScanQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflow")
public class WorkflowScanController {

    private final ScanQueue scanQueue;

    @Autowired
    public WorkflowScanController( ScanQueue scanQueue) {
        this.scanQueue = scanQueue;
    }

    @PostMapping("/scan")
    public ResponseEntity<String> scanWorkFlow(@RequestBody WorkFlowScanRequestDTO request) {
        System.out.println("Received SCAN request with URL: " + request.getUrl());
        scanQueue.addToQueue(request.getUrl());
        scanQueue.printQueue();
        return ResponseEntity.ok("Scan request received!");
    }
}