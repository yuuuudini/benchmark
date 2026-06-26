package com.example.pipeline.scheduler;

import com.example.pipeline.service.PipelineOrchestrator;
import org.slf4j.Logger;         
import org.slf4j.LoggerFactory;  
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "orchestrator.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class PipelineScheduler {

    private static final Logger log = LoggerFactory.getLogger(PipelineScheduler.class);

    private final PipelineOrchestrator pipelineOrchestrator;

    // Standard constructor injection instead of @RequiredArgsConstructor
    public PipelineScheduler(PipelineOrchestrator pipelineOrchestrator) {
        this.pipelineOrchestrator = pipelineOrchestrator;
    }

    @Scheduled(cron = "${orchestrator.scheduler.cron-expression}")
    public void runScheduledSyncAndPipeline() {
        log.info("Triggering scheduled automated Git check and calculation pipeline run.");
        try {
            pipelineOrchestrator.orchestrateScheduledExecution();
        } catch (Exception e) {
            log.error("An error occurred during the scheduled pipeline run context process execution", e);
        }
    }
}