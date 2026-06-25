package com.example.app.git;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "git")
public class GitProperties {

    private List<RepositoryConfig> repositories = new ArrayList<>();

    public List<RepositoryConfig> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<RepositoryConfig> repositories) {
        this.repositories = repositories;
    }

    public static class RepositoryConfig {

        private String name;
        private String repoUrl;
        private String repoPath;
        private String branch;
        private String token;
        private String remote = "origin";

    
    }
}