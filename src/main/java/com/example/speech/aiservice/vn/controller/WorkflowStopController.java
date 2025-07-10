package com.example.speech.aiservice.vn.controller;

import com.example.speech.aiservice.vn.service.workflow.ixdzs8.Ixdzs8ProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflow")
public class WorkflowStopController  {

    private final Ixdzs8ProcessorService ixdzs8ProcessorService;

    @Autowired
    public WorkflowStopController(Ixdzs8ProcessorService ixdzs8ProcessorService) {
        this.ixdzs8ProcessorService = ixdzs8ProcessorService;
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopWorkflow() {
        System.out.println("Received STOP request!");
        ixdzs8ProcessorService.stopConditions();
        return ResponseEntity.ok("Workflow stopped successfully!");
    }
}
