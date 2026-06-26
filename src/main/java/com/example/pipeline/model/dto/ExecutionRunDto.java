package com.example.pipeline.model.dto;

import com.example.pipeline.domain.enumeration.RunStatus;
import java.time.Instant;

public class ExecutionRunDto {
    private Long id;
    private RunStatus status;
    private String datasetCommitHash;
    private String scriptCommitHash;
    private String triggeredBy;
    private Instant startedAt;
    private Instant endedAt;
    private Long durationMs;
    private String algorithmName;
    private String errorMessage;
    // Note: Excluded raw clob/blob logs from default DTO listings for performance optimization

    public ExecutionRunDto() {
    }

    public ExecutionRunDto(Long id, RunStatus status, String datasetCommitHash, String scriptCommitHash,
            String triggeredBy, Instant startedAt, Instant endedAt, Long durationMs, String algorithmName,
            String errorMessage) {
        this.id = id;
        this.status = status;
        this.datasetCommitHash = datasetCommitHash;
        this.scriptCommitHash = scriptCommitHash;
        this.triggeredBy = triggeredBy;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationMs = durationMs;
        this.algorithmName = algorithmName;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public RunStatus getStatus() {
        return status;
    }
    public void setStatus(RunStatus status) {
        this.status = status;
    }
    public String getDatasetCommitHash() {
        return datasetCommitHash;
    }
    public void setDatasetCommitHash(String datasetCommitHash) {
        this.datasetCommitHash = datasetCommitHash;
    }
    public String getScriptCommitHash() {
        return scriptCommitHash;
    }
    public void setScriptCommitHash(String scriptCommitHash) {
        this.scriptCommitHash = scriptCommitHash;
    }
    public String getTriggeredBy() {
        return triggeredBy;
    }
    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }
    public Instant getStartedAt() {
        return startedAt;
    }
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    public Instant getEndedAt() {
        return endedAt;
    }
    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
    public Long getDurationMs() {
        return durationMs;
    }
    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
    public String getAlgorithmName() {
        return algorithmName;
    }
    public void setAlgorithmName(String algorithmName) {
        this.algorithmName = algorithmName;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
}