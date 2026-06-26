package com.example.pipeline.service;

import com.example.pipeline.config.GitProperties;
import com.example.pipeline.domain.entity.ExecutionRun;
import com.example.pipeline.domain.entity.RepositoryState;
import com.example.pipeline.domain.enumeration.RunStatus;
import com.example.pipeline.domain.repository.ExecutionRunRepository;
import com.example.pipeline.domain.repository.RepositoryStateRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class PipelineOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PipelineOrchestrator.class);

    @Autowired
    private GitSyncService gitSyncService;
    @Autowired
    private ExecutionService executionService;
    @Autowired
    private ExecutionRunRepository runRepository;
    @Autowired
    private RepositoryStateRepository repositoryStateRepository; // Added dependency
    @Autowired
    private GitProperties gitProperties;

    @Transactional
    public void orchestrateScheduledExecution() {
        // 1. Use GitSyncService to pull down code/data and extract commits
        String dataHash = gitSyncService.syncRepository("dataset", gitProperties.getRepositories().getDataset());
        String scriptHash = gitSyncService.syncRepository("scripts", gitProperties.getRepositories().getScripts());

        // 2. Optimization check
        boolean alreadyProcessed = runRepository.existsByDatasetCommitHashAndScriptCommitHashAndStatus(
                dataHash, scriptHash, RunStatus.COMPLETED
        );

        if (alreadyProcessed) {
            log.info("Execution skipped. Commits [Data: {}, Scripts: {}] already processed successfully.", dataHash, scriptHash);
            return;
        }

        // 3. Delegate to async execution pipeline
        triggerPipeline(dataHash, scriptHash, "SYSTEM_SCHEDULER", "main_execution.py");
    }

    @Async("pipelineTaskExecutor")
    @Transactional
    public void triggerPipeline(String dataHash, String scriptHash, String initiator, String scriptName) {
        ExecutionRun run = ExecutionRun.builder()
                .status(RunStatus.RUNNING)
                .datasetCommitHash(dataHash)
                .scriptCommitHash(scriptHash)
                .triggeredBy(initiator)
                .startedAt(Instant.now())
                .algorithmName(scriptName)
                .build();

        run = runRepository.save(run);

        String contextScriptPath = gitProperties.getRepositories().getScripts().getLocalPath() + "/" + scriptName;
        String contextDataPath = gitProperties.getRepositories().getDataset().getLocalPath();

        try {
            // 4. Call ExecutionService to run native ProcessBuilder
            ExecutionService.ExecutionResult result = executionService.executePythonScript(
                    contextScriptPath, contextDataPath, Map.of()
            );

            run.setStdoutLog(result.stdout());
            run.setStderrLog(result.stderr());
            run.setDurationMs(result.durationMs());
            run.setEndedAt(Instant.now());
            
            if (result.success()) {
                run.setStatus(RunStatus.COMPLETED);
                
                // 5. Explicitly update RepositoryState tracking on success
                updateRepoState("dataset", dataHash);
                updateRepoState("scripts", scriptHash);
            } else {
                run.setStatus(RunStatus.FAILED);
                run.setErrorMessage("Python runtime script exited with a failure code.");
            }

        } catch (Exception e) {
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage(e.getMessage());
            run.setEndedAt(Instant.now());
        } finally {
            runRepository.save(run);
        }
    }

    private void updateRepoState(String repoKey, String commitHash) {
        RepositoryState state = repositoryStateRepository.findById(repoKey)
                .orElse(RepositoryState.builder().repoKey(repoKey).build());
        state.setLastProcessedCommit(commitHash);
        state.setUpdatedAt(Instant.now());
        repositoryStateRepository.save(state);
    }
}