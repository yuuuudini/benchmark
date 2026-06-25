package com.example.app.update;

import com.example.app.git.GitStatusService;
import org.springframework.stereotype.Service;

@Service
public class UpdateService {
    private final GitStatusService statusService;

    public UpdateService(GitStatusService statusService) {
        this.statusService = statusService;
    }

    public void checkAndUpdate() throws Exception {
        statusService.isUpdateAvailable();
    }
}
