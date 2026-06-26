package com.example.pipeline.domain.entity;

import com.example.pipeline.domain.enumeration.RunStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "execution_batches")
public class ExecutionBatch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private RunStatus status;

    private Instant createdAt;
    private Instant endedAt;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("batch")
    @OrderBy("sequenceOrder ASC")
    private List<ExecutionRun> runs = new ArrayList<>();

    // Constructors
    public ExecutionBatch() {
    }

    public ExecutionBatch(Long id, RunStatus status, Instant createdAt, Instant endedAt, List<ExecutionRun> runs) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
        this.endedAt = endedAt;
        if (runs != null) {
            this.runs = runs;
        }
    }

    // Custom Builder Pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private RunStatus status;
        private Instant createdAt;
        private Instant endedAt;
        private List<ExecutionRun> runs = new ArrayList<>();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder status(RunStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder endedAt(Instant endedAt) { this.endedAt = endedAt; return this; }
        public Builder runs(List<ExecutionRun> runs) { this.runs = runs; return this; }

        public ExecutionBatch build() {
            return new ExecutionBatch(id, status, createdAt, endedAt, runs);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RunStatus getStatus() { return status; }
    public void setStatus(RunStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt;}

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt;}

    public List<ExecutionRun> getRuns() { return runs; }
    public void setRuns(List<ExecutionRun> runs) { this.runs = runs; }
}