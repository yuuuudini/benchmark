package com.example.pipeline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "orchestrator.git")
public class GitProperties {
    private String sshPrivateKeyPath;
    private Repositories repositories = new Repositories();

    
    public GitProperties() {
    }
    
    public static class Repositories {
        private RepoConfig dataset = new RepoConfig();
        private RepoConfig scripts = new RepoConfig();

        public RepoConfig getDataset() {
            return dataset;
        }
        public void setDataset(RepoConfig dataset) {
            this.dataset = dataset;
        }
        public RepoConfig getScripts() {
            return scripts;
        }
        public void setScripts(RepoConfig scripts) {
            this.scripts = scripts;
        }
        
    }
    public static class RepoConfig {
        private String url;
        private String branch;
        private String localPath;

        public String getUrl() {
            return url;
        }
        public void setUrl(String url) {
            this.url = url;
        }
        public String getBranch() {
            return branch;
        }
        public void setBranch(String branch) {
            this.branch = branch;
        }
        public String getLocalPath() {
            return localPath;
        }
        public void setLocalPath(String localPath) {
            this.localPath = localPath;
        }
    }
    public String getSshPrivateKeyPath() {
        return sshPrivateKeyPath;
    }
    public void setSshPrivateKeyPath(String sshPrivateKeyPath) {
        this.sshPrivateKeyPath = sshPrivateKeyPath;
    }
    public Repositories getRepositories() {
        return repositories;
    }
    public void setRepositories(Repositories repositories) {
        this.repositories = repositories;
    }
    
}