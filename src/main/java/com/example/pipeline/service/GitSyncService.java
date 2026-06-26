package com.example.pipeline.service;

import com.example.pipeline.config.GitProperties;
import com.example.pipeline.exception.GitSyncException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
public class GitSyncService {

    private static final Logger log = LoggerFactory.getLogger(GitSyncService.class);
    
    @Autowired
    private GitProperties gitProperties;

    
    public GitSyncService() {
    }

    public synchronized String syncRepository(String repoKey, GitProperties.RepoConfig config) {
        File localDir = new File(config.getLocalPath());
        try {
            if (!localDir.exists()) {
                log.info("Cloning repository {} into {}", repoKey, config.getLocalPath());
                try (Git git = Git.cloneRepository()
                        .setURI(config.getUrl())
                        .setDirectory(localDir)
                        .setBranch(config.getBranch())
                        .setTransportConfigCallback(getSshTransportCallback())
                        .call()) {
                    return git.getRepository().resolve("HEAD").getName();
                }
            } else {
                log.info("Pulling latest changes for repository {} in {}", repoKey, config.getLocalPath());
                try (Git git = Git.open(localDir)) {
                    git.pull()
                       .setRemote("origin")
                       .setRemoteBranchName(config.getBranch())
                       .setTransportConfigCallback(getSshTransportCallback())
                       .call();
                    return git.getRepository().resolve("HEAD").getName();
                }
            }
        } catch (Exception e) {
            log.error("Failed to sync repository: {}", repoKey, e);
            throw new GitSyncException("Git synchronization failed for " + repoKey, e);
        }
    }
public synchronized Map<String, Object> checkForUpdates(String repoKey, GitProperties.RepoConfig config) {
    Map<String, Object> result = new HashMap<>();
    File localDir = new File(config.getLocalPath());

    if (!localDir.exists() || !new File(localDir, ".git").exists()) {
        result.put("status", "NOT_INITIALIZED");
        result.put("has_new_commits", false);
        return result;
    }

    try (Git git = Git.open(localDir)) {
        Repository repository = git.getRepository();
        
        // 1. Get the current local commit hash
        ObjectId localHead = repository.resolve("HEAD");
        String localCommitHash = localHead != null ? localHead.getName() : "UNKNOWN";

        log.info("Fetching remote metadata for check on {}...", repoKey);
        // 2. Fetch remote changes to local cache WITHOUT merging them into the workspace
        git.fetch()
           .setRemote("origin")
           .setTransportConfigCallback(getSshTransportCallback())
           .call();

        // 3. Resolve the remote tracking branch commit hash (e.g., refs/remotes/origin/main)
        String remoteBranchRef = "refs/remotes/origin/" + config.getBranch();
        ObjectId remoteHead = repository.resolve(remoteBranchRef);
        
        if (remoteHead == null) {
            throw new IllegalStateException("Could not resolve remote tracking branch: " + remoteBranchRef);
        }
        
        String remoteCommitHash = remoteHead.getName();
        boolean hasNewCommits = !localCommitHash.equalsIgnoreCase(remoteCommitHash);

        result.put("status", "ACTIVE");
        result.put("local_commit", localCommitHash);
        result.put("remote_latest_commit", remoteCommitHash);
        result.put("has_new_commits", hasNewCommits);

    } catch (Exception e) {
        log.error("Failed checking updates for repo: {}", repoKey, e);
        result.put("status", "ERROR");
        result.put("error", e.getMessage());
        result.put("has_new_commits", false);
    }

    return result;
}

    private TransportConfigCallback getSshTransportCallback() {
        // Use the modern Apache SSHD Session Factory designed for JGit 6.x
        File privateKey = new File(gitProperties.getSshPrivateKeyPath());
        File sshDir = privateKey.getParentFile();

        SshdSessionFactory sshSessionFactory = new SshdSessionFactoryBuilder()
                .setPreferredAuthentications("publickey")
                .setHomeDirectory(FS.DETECTED.userHome())
                .setSshDirectory(sshDir)
                .build(null);

        return transport -> {
            if (transport instanceof SshTransport sshTransport) {
                sshTransport.setSshSessionFactory(sshSessionFactory);
            }
        };
    }
}