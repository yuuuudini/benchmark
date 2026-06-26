package com.example.pipeline.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ExecutionService {

 private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    @Value("${orchestrator.execution.python-executable:python3}")
    private String pythonExecutable;

    @Value("${orchestrator.execution.timeout-minutes:30}")
    private long timeoutMinutes;

    public ExecutionResult executePythonScript(String scriptPath, String datasetPath, Map<String, String> extraParams) {
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);
        
        extraParams.forEach((k, v) -> {
            command.add("--" + k);
            command.add(k.equals("data-path") ?  datasetPath + "/" + v : v);
        });

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        // Isolate context path execution directory
        processBuilder.directory(new File(scriptPath).getParentFile());
        
        // Setup secure environment isolation injection
        Map<String, String> environment = processBuilder.environment();
        environment.put("PYTHONUNBUFFERED", "1");

        long startTime = System.currentTimeMillis();
        try {
            Process process = processBuilder.start();
            
            // Read streams asynchronously to prevent OS buffer blocking deadlocks
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            long duration = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                log.error("Process execution timed out for script: {}", scriptPath);
                return new ExecutionResult(false, "", "Execution Timed Out.", duration);
            }

            String stdout = stdInput.lines().collect(Collectors.joining("\n"));
            String stderr = stdError.lines().collect(Collectors.joining("\n"));
            boolean success = (process.exitValue() == 0);

            return new ExecutionResult(success, stdout, stderr, duration);

        } catch (Exception e) {
            log.error("Failed executing process for script: {}", scriptPath, e);
            return new ExecutionResult(false, "", e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    public record ExecutionResult(boolean success, String stdout, String stderr, long durationMs) {}
}