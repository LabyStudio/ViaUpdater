package de.labystudio.viaupdater.updater.source.sources;

import de.labystudio.viaupdater.api.github.GitHubApi;
import de.labystudio.viaupdater.api.github.model.GitHubProject;
import de.labystudio.viaupdater.api.github.model.artifacts.Artifact;
import de.labystudio.viaupdater.api.github.model.artifacts.Artifacts;
import de.labystudio.viaupdater.api.github.model.runs.Run;
import de.labystudio.viaupdater.updater.ViaProject;
import de.labystudio.viaupdater.updater.builder.ProjectBuilder;
import de.labystudio.viaupdater.updater.source.Source;
import de.labystudio.viaupdater.updater.source.provider.ProviderContext;
import de.labystudio.viaupdater.updater.source.provider.exception.ProviderException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static de.labystudio.viaupdater.updater.source.provider.StatusType.PROGRESS;

public class GitHubSource implements Source<GitHubProject> {

    private final GitHubApi api;

    public GitHubSource(String token) {
        this.api = new GitHubApi(token);
    }

    @Override
    public Path provide(
            ProviderContext context,
            ViaProject project,
            GitHubProject sourceProject
    ) throws ProviderException {
        Path tmpDirectory = context.tmpDirectory();
        Path sourceZip = tmpDirectory.resolve("sources").resolve(project.name() + ".zip");
        Path buildDirectory = tmpDirectory.resolve("repositories").resolve(project.name());
        Path outDirectory = tmpDirectory.resolve("out");

        try {
            if (Files.notExists(sourceZip)) {
                Files.createDirectories(sourceZip);
            }
            if (Files.notExists(tmpDirectory)) {
                Files.createDirectories(tmpDirectory);
            }
            if (Files.notExists(outDirectory)) {
                Files.createDirectories(outDirectory);
            }

            String workflow = sourceProject.workflow();
            if (workflow == null) {
                try {
                    context.updateStatus(PROGRESS, "Downloading source code of " + project.name() + " from GitHub...");
                    this.api.downloadSourceCode(sourceProject, sourceZip);

                    context.updateStatus(PROGRESS, "Building " + project.name() + " project...");
                    Path builtJar = new ProjectBuilder(sourceZip, buildDirectory).build();

                    Path out = outDirectory.resolve(builtJar.getFileName());
                    Files.copy(builtJar, out, StandardCopyOption.REPLACE_EXISTING);
                    return out;
                } catch (IOException e) {
                    throw new ProviderException("Failed to provide GitHub source", e);
                }
            } else {
                try {
                    Run run = this.api.requestLatestWorkflowRun(sourceProject);
                    Artifacts artifacts = this.api.requestArtifacts(run);
                    Artifact artifact = artifacts.artifacts().getFirst();

                    Path out = outDirectory.resolve(artifact.name());
                    this.api.writeArtifact(artifact, out);
                    return out;
                } catch (Exception e) {
                    throw new ProviderException("Failed to fetch workflow runs for project " + project.name(), e);
                }
            }
        } catch (ProviderException e) {
            throw e;
        } catch (IOException e) {
            throw new ProviderException("Failed to provide GitHub source", e);
        }
    }
}

