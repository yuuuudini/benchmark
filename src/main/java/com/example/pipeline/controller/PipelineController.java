package com.example.pipeline.controller;

import com.example.pipeline.config.GitProperties;
import com.example.pipeline.domain.entity.ExecutionRun;
import com.example.pipeline.domain.repository.ExecutionRunRepository;
import com.example.pipeline.model.request.TriggerRunRequest;
import com.example.pipeline.service.GitSyncService;
import com.example.pipeline.service.PipelineOrchestrator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PipelineController {

    @Autowired
    private PipelineOrchestrator orchestrator;
    @Autowired
    private GitSyncService gitSyncService;
    @Autowired
    private ExecutionRunRepository runRepository;
    @Autowired
    private GitProperties gitProperties;

    @PostMapping("/runs")
    public ResponseEntity<String> triggerRun(@RequestBody TriggerRunRequest request) {
        // Sync repos to ensure we are running against current files
        String dataHash = gitSyncService.syncRepository("dataset", gitProperties.getRepositories().getDataset());
        String scriptHash = gitSyncService.syncRepository("scripts", gitProperties.getRepositories().getScripts());
        
        String scriptToRun = (request.getScriptName() != null) ? request.getScriptName() : "main_execution.py";
        
        // Triggers async pipeline task executor loop
        orchestrator.triggerPipeline(dataHash, scriptHash, "REST_API_USER", scriptToRun, request.getParameters());
        return ResponseEntity.accepted().body("Pipeline execution run scheduled successfully via user demand.");
    }

    @GetMapping("/runs")
    public ResponseEntity<List<ExecutionRun>> listAllRuns() {
        return ResponseEntity.ok(runRepository.findAll());
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<ExecutionRun> getRunDetails(@PathVariable Long id) {
        return runRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> manualGitSync() {
        String dataHash = gitSyncService.syncRepository("dataset", gitProperties.getRepositories().getDataset());
        String scriptHash = gitSyncService.syncRepository("scripts", gitProperties.getRepositories().getScripts());
        return ResponseEntity.ok(Map.of("dataset_head", dataHash, "scripts_head", scriptHash));
    }
}