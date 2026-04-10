package de.labystudio.viaupdater.api.github.model;

import de.labystudio.viaupdater.updater.source.SourceProject;

public record GitHubProject(
        String owner,
        String repository,
        String branch,
        String workflow
) implements SourceProject {
}
