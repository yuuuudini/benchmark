package com.example.app.git;

import com.example.app.update.UpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/git")
public class GitController {

    private final GitStatusService gitStatusService;
    private final UpdateService updateService;

    public GitController(
            GitStatusService gitStatusService,
            UpdateService updateService) {
        this.gitStatusService = gitStatusService;
        this.updateService = updateService;
    }

    @GetMapping("/{repository}/status")
    public ResponseEntity<String> status(
            @PathVariable String repository) throws Exception {

        return ResponseEntity.ok(
                gitStatusService.getStatus(repository)
        );
    }

    @GetMapping("/{repository}/update-available")
    public ResponseEntity<Boolean> updateAvailable(
            @PathVariable String repository) throws Exception {

        return ResponseEntity.ok(
                gitStatusService.isUpdateAvailable(repository)
        );
    }

    @PostMapping("/{repository}/update")
    public ResponseEntity<String> update(
            @PathVariable String repository) throws Exception {

        updateService.checkAndUpdate(repository);

        return ResponseEntity.ok(
                "Update triggered for repository: " + repository
        );
    }
}
