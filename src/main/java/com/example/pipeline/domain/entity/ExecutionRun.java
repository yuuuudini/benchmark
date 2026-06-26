package com.example.pipeline.domain.entity;

import com.example.pipeline.domain.enumeration.RunStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "execution_runs", indexes = {
    @Index(name = "idx_run_status", columnList = "status"),
    @Index(name = "idx_hashes", columnList = "dataset_commit_hash, script_commit_hash")
})
public class ExecutionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status;

    @Column(name = "dataset_commit_hash", nullable = false, length = 40)
    private String datasetCommitHash;

    @Column(name = "script_commit_hash", nullable = false, length = 40)
    private String scriptCommitHash;

    @Column(name = "triggered_by", nullable = false, length = 50)
    private String triggeredBy;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "algorithm_name", length = 100)
    private String algorithmName;

    @Lob
    @Column(name = "stdout_log", columnDefinition = "LONGTEXT") 
    private String stdoutLog;

    @Lob
    @Column(name = "stderr_log", columnDefinition = "LONGTEXT") 
    private String stderrLog;
    
    @Column(name = "error_message")
    private String errorMessage;

    // Default Constructor
    public ExecutionRun() {
    }

    // All-Args Constructor
    public ExecutionRun(Long id, RunStatus status, String datasetCommitHash, String scriptCommitHash,
            String triggeredBy, Instant startedAt, Instant endedAt, Long durationMs, String algorithmName,
            String stdoutLog, String stderrLog, String errorMessage) {
        this.id = id;
        this.status = status;
        this.datasetCommitHash = datasetCommitHash;
        this.scriptCommitHash = scriptCommitHash;
        this.triggeredBy = triggeredBy;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationMs = durationMs;
        this.algorithmName = algorithmName;
        this.stdoutLog = stdoutLog;
        this.stderrLog = stderrLog;
        this.errorMessage = errorMessage;
    }

    // Static entry point to initiate builder flow
    public static Builder builder() {
        return new Builder();
    }

    // --- STATIC INNER BUILDER CLASS ---
    public static class Builder {
        private Long id;
        private RunStatus status;
        private String datasetCommitHash;
        private String scriptCommitHash;
        private String triggeredBy;
        private Instant startedAt;
        private Instant endedAt;
        private Long durationMs;
        private String algorithmName;
        private String stdoutLog;
        private String stderrLog;
        private String errorMessage;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder status(RunStatus status) {
            this.status = status;
            return this;
        }

        public Builder datasetCommitHash(String datasetCommitHash) {
            this.datasetCommitHash = datasetCommitHash;
            return this;
        }

        public Builder scriptCommitHash(String scriptCommitHash) {
            this.scriptCommitHash = scriptCommitHash;
            return this;
        }

        public Builder triggeredBy(String triggeredBy) {
            this.triggeredBy = triggeredBy;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder endedAt(Instant endedAt) {
            this.endedAt = endedAt;
            return this;
        }

        public Builder durationMs(Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder algorithmName(String algorithmName) {
            this.algorithmName = algorithmName;
            return this;
        }

        public Builder stdoutLog(String stdoutLog) {
            this.stdoutLog = stdoutLog;
            return this;
        }

        public Builder stderrLog(String stderrLog) {
            this.stderrLog = stderrLog;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ExecutionRun build() {
            return new ExecutionRun(id, status, datasetCommitHash, scriptCommitHash, triggeredBy, 
                                    startedAt, endedAt, durationMs, algorithmName, stdoutLog, 
                                    stderrLog, errorMessage);
        }
    }

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RunStatus getStatus() { return status; }
    public void setStatus(RunStatus status) { this.status = status; }

    public String getDatasetCommitHash() { return datasetCommitHash; }
    public void setDatasetCommitHash(String datasetCommitHash) { this.datasetCommitHash = datasetCommitHash; }

    public String getScriptCommitHash() { return scriptCommitHash; }
    public void setScriptCommitHash(String scriptCommitHash) { this.scriptCommitHash = scriptCommitHash; }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getAlgorithmName() { return algorithmName; }
    public void setAlgorithmName(String algorithmName) { this.algorithmName = algorithmName; }

    public String getStdoutLog() { return stdoutLog; }
    public void setStdoutLog(String stdoutLog) { this.stdoutLog = stdoutLog; }

    public String getStderrLog() { return stderrLog; }
    public void setStderrLog(String stderrLog) { this.stderrLog = stderrLog; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}