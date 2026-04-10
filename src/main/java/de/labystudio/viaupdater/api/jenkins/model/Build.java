package de.labystudio.viaupdater.api.jenkins.model;

public record Build(
        Artifact[] artifacts,
        String url
) {
}
