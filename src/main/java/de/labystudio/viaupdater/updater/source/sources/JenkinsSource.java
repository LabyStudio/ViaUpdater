package de.labystudio.viaupdater.updater.source.sources;

import de.labystudio.viaupdater.api.jenkins.Jenkins;
import de.labystudio.viaupdater.api.jenkins.model.Artifact;
import de.labystudio.viaupdater.api.jenkins.model.Build;
import de.labystudio.viaupdater.api.jenkins.model.JenkinsProject;
import de.labystudio.viaupdater.updater.ViaProject;
import de.labystudio.viaupdater.updater.source.Source;
import de.labystudio.viaupdater.updater.source.provider.ProviderContext;
import de.labystudio.viaupdater.updater.source.provider.exception.ProviderException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static de.labystudio.viaupdater.updater.source.provider.StatusType.PROGRESS;

public class JenkinsSource implements Source<JenkinsProject> {

    private final Jenkins jenkins;

    public JenkinsSource(String endpoint) {
        this.jenkins = new Jenkins(endpoint);
    }

    @Override
    public Path provide(
            ProviderContext context,
            ViaProject project,
            JenkinsProject jenkinsProject
    ) throws ProviderException {
        try {
            Path outDirectory = context.tmpDirectory().resolve("out");
            if (Files.notExists(outDirectory)) {
                Files.createDirectories(outDirectory);
            }

            context.updateStatus(PROGRESS, "Fetching Jenkins build information for " + project.name() + "...");

            Build build = this.jenkins.requestLastSuccessfulBuild(jenkinsProject.project());
            if (build.artifacts().length == 0) {
                throw new ProviderException("No artifacts found");
            }

            Artifact artifact = build.artifacts()[0];
            context.updateStatus(PROGRESS, "Downloading artifact " + artifact.fileName() + " from Jenkins...");

            Path out = outDirectory.resolve(artifact.fileName());
            this.jenkins.writeArtifact(build, artifact, out);
            return out;
        } catch (IOException e) {
            throw new ProviderException("Failed to provide Jenkins source", e);
        }
    }

}
