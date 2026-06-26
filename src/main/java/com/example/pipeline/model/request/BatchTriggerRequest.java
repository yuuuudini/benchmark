package com.example.pipeline.model.request;

import java.util.ArrayList;
import java.util.List;

public class BatchTriggerRequest {
    
    private List<TriggerRunRequest> jobs = new ArrayList<>();

    public BatchTriggerRequest() {
    }

    public BatchTriggerRequest(List<TriggerRunRequest> jobs) {
        if (jobs != null) {
            this.jobs = jobs;
        }
    }

    public List<TriggerRunRequest> getJobs() {
        return jobs;
    }

    public void setJobs(List<TriggerRunRequest> jobs) {
        if (jobs != null) {
            this.jobs = jobs;
        } else {
            this.jobs = new ArrayList<>();
        }
    }
}