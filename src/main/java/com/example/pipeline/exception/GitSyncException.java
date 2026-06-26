package com.example.pipeline.exception;

public class GitSyncException extends RuntimeException {
    public GitSyncException(String message) {
        super(message);
    }
    public GitSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}