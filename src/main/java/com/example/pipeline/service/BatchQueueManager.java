package com.example.pipeline.service;

import com.example.pipeline.domain.entity.ExecutionBatch;
import com.example.pipeline.domain.entity.ExecutionRun;
import com.example.pipeline.domain.enumeration.RunStatus;
import com.example.pipeline.domain.repository.ExecutionBatchRepository;
import com.example.pipeline.domain.repository.ExecutionRunRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class BatchQueueManager {
    
    private static final Logger log = LoggerFactory.getLogger(BatchQueueManager.class);

    @Autowired 
    private ExecutionBatchRepository batchRepository;
    
    @Autowired 
    private ExecutionRunRepository runRepository;
    
    @Autowired 
    private PipelineOrchestrator orchestrator;

    /**
     * Spawns a background thread task context to process a batch sequence line-by-line.
     * Keeps execution strictly isolated to a single worker flow.
     */
    @Async("pipelineTaskExecutor")
    @Transactional
    public void processBatchSequentially(Long batchId) {
        // Transaction Isolation Guard: Poll until the web thread transaction completely commits
        ExecutionBatch batch = null;
        int retries = 0;
        
        while (batch == null && retries < 5) {
            batch = batchRepository.findById(batchId).orElse(null);
            if (batch == null) {
                try {
                    log.info("Batch {} not visible yet (waiting for DB transaction commit...). Retrying...", batchId);
                    Thread.sleep(300); // Sleep 300ms to allow HTTP thread to finish returning its response
                    retries++;
                } catch (InterruptedException e) {
                    log.error("Batch tracking recovery loop interrupted for ID {}", batchId, e);
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        if (batch == null) {
            log.error("Batch sequence aborted. Id {} could not be found after database sync retries.", batchId);
            return;
        }

        // Flip master batch state from PENDING to RUNNING
        batch.setStatus(RunStatus.RUNNING);
        batchRepository.save(batch);

        boolean sequenceBroken = false;

        // Loop through child tasks (Sorted automatically by sequenceOrder via @OrderBy)
        for (ExecutionRun run : batch.getRuns()) {
            
            // If an earlier script crashed, cleanly skip subsequent sequence files
            if (sequenceBroken) {
                run.setStatus(RunStatus.FAILED);
                run.setErrorMessage("Skipped. Execution chain broken due to preceding step failure.");
                runRepository.save(run);
                continue;
            }

            log.info("Batch {}: Launching sequence step {} of {}", 
                    batchId, run.getSequenceOrder() + 1, batch.getRuns().size());
            
            try {
                // Pass execution to orchestrator synchronously on this background thread loop
                orchestrator.executeSingleRunSynchronously(run);
                
                // Re-fetch state directly to verify if the python process exited with success or error
                if (run.getStatus() == RunStatus.FAILED) {
                    log.warn("Batch {}: Script failed at step position {}. Breaking sequence loop.", 
                            batchId, run.getSequenceOrder());
                    sequenceBroken = true;
                }
            } catch (Exception e) {
                log.error("Batch {}: Unexpected driver exception at sequence step {}", batchId, run.getSequenceOrder(), e);
                sequenceBroken = true;
                run.setStatus(RunStatus.FAILED);
                run.setErrorMessage("Orchestration pipeline exception: " + e.getMessage());
                runRepository.save(run);
            }
        }

        // Finalize the master batch pipeline state record
        batch.setStatus(sequenceBroken ? RunStatus.FAILED : RunStatus.COMPLETED);
        batch.setEndedAt(Instant.now());
        batchRepository.save(batch);
        
        log.info("Batch sequence processing finished for ID: {}. Outcome: {}", batchId, batch.getStatus());
    }
}