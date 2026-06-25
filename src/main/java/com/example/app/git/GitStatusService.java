package com.example.app.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

@Service
public class GitStatusService {
    private final GitProperties props;
    private final GitBootstrapService bootstrap;

    public GitStatusService(GitProperties props, GitBootstrapService bootstrap) {
        this.props = props;
        this.bootstrap = bootstrap;
    }

    public boolean isUpdateAvailable() throws Exception {
        try (Git git = bootstrap.initRepo()) {
            git.fetch().setRemote(props.getRemote()).call();

            ObjectId local = git.getRepository().resolve("HEAD");
            ObjectId remote = git.getRepository().resolve(
                    "refs/remotes/" + props.getRemote() + "/" + props.getBranch());

            return local == null || remote == null || !local.equals(remote);
        }
    }

    public String getStatus() throws Exception {
        return isUpdateAvailable() ? "OUTDATED" : "UP_TO_DATE";
    }
}
