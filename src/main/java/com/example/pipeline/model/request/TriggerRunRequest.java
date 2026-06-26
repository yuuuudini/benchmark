package com.example.pipeline.model.request;

import java.util.Map;

public class TriggerRunRequest {
    private String scriptName;
    private Map<String, String> parameters;
    
    public TriggerRunRequest(String scriptName, Map<String, String> parameters) {
        this.scriptName = scriptName;
        this.parameters = parameters;
    }
    public TriggerRunRequest() {
    }
    public String getScriptName() {
        return scriptName;
    }
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }
    public Map<String, String> getParameters() {
        return parameters;
    }
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
    
}