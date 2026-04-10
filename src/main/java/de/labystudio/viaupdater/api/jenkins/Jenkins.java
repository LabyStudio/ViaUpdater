// Decompiled with: FernFlower
// Class Version: 17
package de.labystudio.viaupdater.api.jenkins;

import de.labystudio.viaupdater.api.jenkins.model.Artifact;
import de.labystudio.viaupdater.api.jenkins.model.Build;
import de.labystudio.viaupdater.util.HttpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Jenkins {

    private static final String URL_LAST_SUCCESSFUL_BUILD = "%s/job/%s/lastSuccessfulBuild/api/json";

    private final String endpoint;

    public Jenkins(String endpoint) {
        this.endpoint = endpoint;
    }

    public Build requestLastSuccessfulBuild(String job) throws IOException {
        return HttpUtils.request(String.format(URL_LAST_SUCCESSFUL_BUILD, this.endpoint, job.replace(" ", "%20")), Build.class);
    }

    public void writeArtifact(Build build, Artifact artifact, Path file) throws IOException {
        String url = build.url() + "/artifact/" + artifact.relativePath();
        try (InputStream in = this.openInputStream(url)) {
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public InputStream openInputStream(String url) throws IOException {
        return HttpUtils.openInputStream(url);
    }
}
