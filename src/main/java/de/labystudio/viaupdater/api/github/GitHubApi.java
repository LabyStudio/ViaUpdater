package de.labystudio.viaupdater.api.github;

import de.labystudio.viaupdater.api.github.model.GitHubProject;
import de.labystudio.viaupdater.api.github.model.artifacts.Artifact;
import de.labystudio.viaupdater.api.github.model.artifacts.Artifacts;
import de.labystudio.viaupdater.api.github.model.runs.Run;
import de.labystudio.viaupdater.api.github.model.runs.WorkflowRuns;
import de.labystudio.viaupdater.util.HttpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class GitHubApi {

    private static final String URL_HOST = "https://api.github.com";
    private static final String URL_WORKFLOW_RUNS = URL_HOST + "/repos/%s/%s/actions/workflows/%s/runs";
    private static final String URL_ZIPBALL = URL_HOST + "/repos/%s/%s/zipball/%s";

    private final String token;

    public GitHubApi(String token) {
        this.token = token;
    }

    public void writeArtifact(Artifact artifact, Path file) throws Exception {
        try (InputStream in = this.openInputStream(artifact.archiveDownloadUrl())) {
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Artifacts requestArtifacts(Run run) throws Exception {
        return HttpUtils.request(run.artifactsUrl(), Artifacts.class);
    }

    public Run requestLatestWorkflowRun(GitHubProject project) throws Exception {
        if (project.workflow() == null) {
            throw new IllegalArgumentException("Workflow is not set for project " + project.repository());
        }
        String url = String.format(URL_WORKFLOW_RUNS, project.owner(), project.repository(), project.workflow());
        WorkflowRuns runs = HttpUtils.request(url, WorkflowRuns.class);
        for (Run run : runs.workflowRuns()) {
            if (run.headBranch().equals(project.branch())) {
                return run;
            }
        }
        return null;
    }

    public void downloadSourceCode(GitHubProject project, Path file) throws IOException {
        String url = String.format(URL_ZIPBALL, project.owner(), project.repository(), project.branch());
        try (InputStream in = this.openInputStream(url)) {
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public InputStream openInputStream(String url) throws IOException {
        if (this.token == null || this.token.isBlank() || this.token.startsWith("<")) {
            throw new IllegalArgumentException("GitHub token is not set");
        }
        return HttpUtils.openInputStream(url, Map.of(
                "Authorization", "Bearer " + this.token,
                "Accept", "application/vnd.github+json",
                "X-GitHub-Api-Version", "2022-11-28"
        ));
    }
}
