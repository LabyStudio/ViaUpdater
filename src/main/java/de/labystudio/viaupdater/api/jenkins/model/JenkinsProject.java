package de.labystudio.viaupdater.api.jenkins.model;

import de.labystudio.viaupdater.updater.source.SourceProject;

public record JenkinsProject(
        String project
) implements SourceProject {
}
