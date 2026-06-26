package com.example.pipeline.service;

import com.example.pipeline.config.GitProperties;
import com.example.pipeline.exception.GitSyncException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

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