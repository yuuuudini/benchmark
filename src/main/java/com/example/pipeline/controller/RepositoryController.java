package com.example.pipeline.controller;

import com.example.pipeline.config.GitProperties;
import com.example.pipeline.domain.repository.RepositoryStateRepository;
import com.example.pipeline.domain.entity.RepositoryState;
import com.example.pipeline.service.GitSyncService;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/repos")
public class RepositoryController {

    @Autowired
    private RepositoryStateRepository repositoryStateRepository;
    @Autowired   
    private GitProperties gitProperties;
    @Autowired
    private GitSyncService gitSyncService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getRepositoryStatus() {
        Map<String, Object> statusMap = new HashMap<>();
        
        statusMap.put("dataset", getLocalRepoDetails(
            "dataset", 
            gitProperties.getRepositories().getDataset().getLocalPath()
        ));
        
        statusMap.put("scripts", getLocalRepoDetails(
            "scripts", 
            gitProperties.getRepositories().getScripts().getLocalPath()
        ));

        return ResponseEntity.ok(statusMap);
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncRepositories() {
        Map<String, Object> responseMap = new HashMap<>();
        
        // Match your service signature exactly by passing the Config object blocks
        responseMap.put("dataset", executeSyncWithChangeDetection(
            "dataset", 
            gitProperties.getRepositories().getDataset()
        ));

        responseMap.put("scripts", executeSyncWithChangeDetection(
            "scripts", 
            gitProperties.getRepositories().getScripts()
        ));

        return ResponseEntity.ok(responseMap);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkRepositoriesForUpdates() {
        Map<String, Object> checkMap = new HashMap<>();
        
        // Check dataset repo
        checkMap.put("dataset", gitSyncService.checkForUpdates(
            "dataset", 
            gitProperties.getRepositories().getDataset()
        ));

        // Check scripts repo
        checkMap.put("scripts", gitSyncService.checkForUpdates(
            "scripts", 
            gitProperties.getRepositories().getScripts()
        ));

        return ResponseEntity.ok(checkMap);
    }

    private Map<String, Object> getLocalRepoDetails(String repoKey, String localPath) {
        Map<String, Object> details = new HashMap<>();
        File repoDir = new File(localPath);
        
        if (!repoDir.exists() || !new File(repoDir, ".git").exists()) {
            details.put("status", "NOT_INITIALIZED");
            details.put("current_commit", "UNKNOWN");
            return details;
        }

        try (Git git = Git.open(repoDir)) {
            String currentCommit = git.getRepository().resolve("HEAD").getName();
            details.put("status", "ACTIVE");
            details.put("current_commit", currentCommit);
            
            repositoryStateRepository.findById(repoKey).ifPresent(state -> {
                details.put("last_processed_commit", state.getLastProcessedCommit());
                details.put("last_sync_timestamp", state.getUpdatedAt());
            });
            
        } catch (Exception e) {
            details.put("status", "ERROR");
            details.put("error", e.getMessage());
        }
        
        return details;
    }

    // This method now matches your exact GitSyncService.syncRepository signature!
    private Map<String, Object> executeSyncWithChangeDetection(String repoKey, GitProperties.RepoConfig config) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. Resolve local pre-sync commit tracking if the local directory exists
            String oldCommit = "NONE";
            File repoDir = new File(config.getLocalPath());
            if (repoDir.exists() && new File(repoDir, ".git").exists()) {
                try (Git git = Git.open(repoDir)) {
                    oldCommit = git.getRepository().resolve("HEAD").getName();
                }
            }

            // 2. Call your exact GitSyncService method block
            String currentCommit = gitSyncService.syncRepository(repoKey, config);

            // 3. Keep database metadata aligned 
            RepositoryState updatedState = RepositoryState.builder()
                .repoKey(repoKey)
                .lastProcessedCommit(currentCommit)
                .updatedAt(Instant.now())
                .build();
            repositoryStateRepository.save(updatedState);

            // 4. Output validation mapping
            result.put("status", "SUCCESS");
            result.put("previous_commit", oldCommit);
            result.put("current_commit", currentCommit);
            result.put("has_new_commits", !oldCommit.equalsIgnoreCase(currentCommit));

        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }
        return result;
    }
}