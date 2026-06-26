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

import java.io.File;
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
    private RepositoryStateRepository repositoryStateRepository;
    @Autowired
    private GitProperties gitProperties;

    @Transactional
    public void orchestrateScheduledExecution() {
        // Use GitSyncService to pull down code/data and extract commits
        String dataHash = gitSyncService.syncRepository("dataset", gitProperties.getRepositories().getDataset());
        String scriptHash = gitSyncService.syncRepository("scripts", gitProperties.getRepositories().getScripts());

        boolean alreadyProcessed = runRepository.existsByDatasetCommitHashAndScriptCommitHashAndStatus(
                dataHash, scriptHash, RunStatus.COMPLETED
        );

        if (alreadyProcessed) {
            log.info("Execution skipped. Commits [Data: {}, Scripts: {}] already processed successfully.", dataHash, scriptHash);
            return;
        }

        triggerPipeline(dataHash, scriptHash, "SYSTEM_SCHEDULER", "main_execution.py", Map.of());
    }

    @Async("pipelineTaskExecutor")
    @Transactional
    public void triggerPipeline(String dataHash, String scriptHash, String initiator, String scriptName, Map<String, String> parameters) {
        ExecutionRun run = ExecutionRun.builder()
                .status(RunStatus.RUNNING)
                .datasetCommitHash(dataHash)
                .scriptCommitHash(scriptHash)
                .triggeredBy(initiator)
                .startedAt(Instant.now())
                .algorithmName(scriptName)
                .parameters(parameters)
                .build();

        run = runRepository.save(run);

        File scriptRepoDir = new File(gitProperties.getRepositories().getScripts().getLocalPath());
        File datasetRepoDir = new File(gitProperties.getRepositories().getDataset().getLocalPath());

        File absoluteScriptFile = new File(scriptRepoDir, new File(scriptName).getName());

        String contextScriptPath = absoluteScriptFile.getAbsolutePath();
        String contextDataPath = datasetRepoDir.getAbsolutePath();
        // -----------------------------------------

        try {
            // Call ExecutionService to run native ProcessBuilder
            ExecutionService.ExecutionResult result = executionService.executePythonScript(
                    contextScriptPath, contextDataPath, parameters
            );

            run.setStdoutLog(result.stdout());
            run.setStderrLog(result.stderr());
            run.setDurationMs(result.durationMs());
            run.setEndedAt(Instant.now());
            
            if (result.success()) {
                run.setStatus(RunStatus.COMPLETED);
                
                // Explicitly update RepositoryState tracking on success
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

    /**
     * Executes a single pipeline run synchronously within the current background worker thread.
     * Used by the sequential batch manager to guarantee precise chronological order.
     */
    public void executeSingleRunSynchronously(ExecutionRun run) {
        // Initialize execution metadata tracking
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        runRepository.save(run);

        // Fetch baseline repository root paths from system configurations
        File scriptRepoDir = new File(gitProperties.getRepositories().getScripts().getLocalPath());
        File datasetRepoDir = new File(gitProperties.getRepositories().getDataset().getLocalPath());

        // Isolate file mappings cleanly to ignore nested input directory prefixes
        String scriptFileName = run.getAlgorithmName() != null ? run.getAlgorithmName() : "main_execution.py";
        File absoluteScriptFile = new File(scriptRepoDir, new File(scriptFileName).getName());
        
        String contextDataPath = datasetRepoDir.getAbsolutePath();

        try {
            log.info("Batch Execution Pipeline: Running {} synchronously on dataset path {}", 
                    absoluteScriptFile.getName(), contextDataPath);

            // Pull runtime variables out of the database collection instead of empty map maps
            Map<String, String> contextParams = run.getParameters();

            ExecutionService.ExecutionResult result = executionService.executePythonScript(
                    absoluteScriptFile.getAbsolutePath(), 
                    contextDataPath, 
                    contextParams
            );

            // Collect console logs, duration metrics, and process termination codes
            run.setStdoutLog(result.stdout());
            run.setStderrLog(result.stderr());
            run.setDurationMs(result.durationMs());
            run.setEndedAt(Instant.now());
            
            if (result.success()) {
                run.setStatus(RunStatus.COMPLETED);
            } else {
                run.setStatus(RunStatus.FAILED);
                run.setErrorMessage("Python runtime process exited with an error code status.");
                log.error("Sync script execution failed for Run ID {}. Stderr: {}", run.getId(), result.stderr());
            }
        } catch (Exception e) {
            // Handle unexpected OS architecture environment crashes or interruptions
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage("Pipeline runtime engine failure: " + e.getMessage());
            run.setEndedAt(Instant.now());
            log.error("Critical error while executing Run ID {} synchronously", run.getId(), e);
        } finally {
            // Flush the operational outcome straight to database
            runRepository.save(run);
        }
    }
}