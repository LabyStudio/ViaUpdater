package de.labystudio.viaupdater.api.jenkins.model;

public record Artifact(
        String displayPath,
        String fileName,
        String relativePath
) {
}
