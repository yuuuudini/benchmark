package com.example.app.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class GitBootstrapService {

    private final GitProperties properties;

    public GitBootstrapService(GitProperties properties) {
        this.properties = properties;
    }

    public Git initRepo(String repositoryName) throws Exception {

        GitProperties.RepositoryConfig repo =
                properties.getRepositories()
                          .stream()
                          .filter(r -> r.getName().equals(repositoryName))
                          .findFirst()
                          .orElseThrow();

        File repoDir = new File(repo.getRepoPath());

        if (!repoDir.exists()) {

            return Git.cloneRepository()
                    .setURI(repo.getRepoUrl())
                    .setDirectory(repoDir)
                    .setBranch(repo.getBranch())
                    .call();
        }

        return Git.open(repoDir);
    }
}
