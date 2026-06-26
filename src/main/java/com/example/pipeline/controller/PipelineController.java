package com.example.pipeline.controller;

import com.example.pipeline.config.GitProperties;
import com.example.pipeline.domain.entity.ExecutionBatch;
import com.example.pipeline.domain.entity.ExecutionRun;
import com.example.pipeline.domain.enumeration.RunStatus;
import com.example.pipeline.domain.repository.ExecutionBatchRepository;
import com.example.pipeline.domain.repository.ExecutionRunRepository;
import com.example.pipeline.model.request.BatchTriggerRequest;
import com.example.pipeline.model.request.TriggerRunRequest;
import com.example.pipeline.service.BatchQueueManager;                  
import com.example.pipeline.service.GitSyncService;
import com.example.pipeline.service.PipelineOrchestrator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
    private ExecutionBatchRepository batchRepository; 
    @Autowired
    private BatchQueueManager batchQueueManager;       
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

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> triggerBatchJob(@RequestBody BatchTriggerRequest request) {
        // Fetch current repository states
        String dataHash = gitSyncService.syncRepository("dataset", gitProperties.getRepositories().getDataset());
        String scriptHash = gitSyncService.syncRepository("scripts", gitProperties.getRepositories().getScripts());

        // Build the parent batch object using your custom builder
        ExecutionBatch batch = ExecutionBatch.builder()
                .status(RunStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        // Populate internal runs with positional index values and parameter maps
        int order = 0;
        for (TriggerRunRequest jobReq : request.getJobs()) {
            
            Map<String, String> jobParams = jobReq.getParameters();
            if (jobParams == null) {
                jobParams = new HashMap<>();
            }

            ExecutionRun run = ExecutionRun.builder()
                .status(RunStatus.PENDING)
                .algorithmName(jobReq.getScriptName())
                .datasetCommitHash(dataHash)
                .scriptCommitHash(scriptHash)
                .triggeredBy("BATCH_API")
                .parameters(jobParams) 
                .startedAt(Instant.now())
                .build();
            
            // Connect the references manually
            run.setSequenceOrder(order++);
            run.setBatch(batch);
            
            batch.getRuns().add(run);
        }

        // Force immediate hard flush straight to DB layer so background worker visibility loop catches it
        ExecutionBatch savedBatch = batchRepository.saveAndFlush(batch);
        batchQueueManager.processBatchSequentially(savedBatch.getId());

        return ResponseEntity.ok(Map.of(
            "status", "QUEUED",
            "batch_id", savedBatch.getId(),
            "total_jobs", savedBatch.getRuns().size()
        ));
    }

    @GetMapping("/batch/{id}")
    public ResponseEntity<ExecutionBatch> getBatchDetails(@PathVariable Long id) {
        return batchRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}