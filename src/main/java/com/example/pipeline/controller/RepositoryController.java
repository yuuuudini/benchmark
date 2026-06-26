package com.example.pipeline.controller;

import com.example.pipeline.config.GitProperties;
import com.example.pipeline.domain.repository.RepositoryStateRepository;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/repos")
public class RepositoryController {

    @Autowired
    private RepositoryStateRepository repositoryStateRepository;
    @Autowired   
    private GitProperties gitProperties;

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
            
            // Supplement with DB state cache metadata if available
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
}