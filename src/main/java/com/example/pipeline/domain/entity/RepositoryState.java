package com.example.pipeline.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "repository_states")
public class RepositoryState {

    @Id
    @Column(name = "repo_key", length = 50)
    private String repoKey; 

    @Column(name = "last_processed_commit", nullable = false, length = 40)
    private String lastProcessedCommit;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default Constructor
    public RepositoryState() {
    }
    
    // All-Args Constructor
    public RepositoryState(String repoKey, String lastProcessedCommit, Instant updatedAt) {
        this.repoKey = repoKey;
        this.lastProcessedCommit = lastProcessedCommit;
        this.updatedAt = updatedAt;
    }

    // Static entry point to initiate builder chain
    public static Builder builder() {
        return new Builder();
    }

    // --- STATIC INNER BUILDER CLASS ---
    public static class Builder {
        private String repoKey;
        private String lastProcessedCommit;
        private Instant updatedAt;

        public Builder repoKey(String repoKey) {
            this.repoKey = repoKey;
            return this;
        }

        public Builder lastProcessedCommit(String lastProcessedCommit) {
            this.lastProcessedCommit = lastProcessedCommit;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public RepositoryState build() {
            return new RepositoryState(repoKey, lastProcessedCommit, updatedAt);
        }
    }

    // --- GETTERS AND SETTERS ---
    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    public String getLastProcessedCommit() {
        return lastProcessedCommit;
    }

    public void setLastProcessedCommit(String lastProcessedCommit) {
        this.lastProcessedCommit = lastProcessedCommit;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}