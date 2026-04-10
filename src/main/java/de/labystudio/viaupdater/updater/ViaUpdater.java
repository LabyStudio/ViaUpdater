package de.labystudio.viaupdater.updater;

import de.labystudio.viaupdater.updater.exception.UpdateException;
import de.labystudio.viaupdater.updater.source.Source;
import de.labystudio.viaupdater.updater.source.SourceRegistry;
import de.labystudio.viaupdater.updater.source.provider.ProviderContext;
import de.labystudio.viaupdater.updater.source.provider.exception.ProviderException;
import de.labystudio.viaupdater.updater.source.sources.GitHubSource;
import de.labystudio.viaupdater.updater.source.sources.JenkinsSource;
import org.jspecify.annotations.NonNull;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

import static de.labystudio.viaupdater.updater.source.provider.StatusType.PROGRESS;
import static de.labystudio.viaupdater.updater.source.provider.StatusType.SUCCESS;

public class ViaUpdater {

    private final SourceRegistry sourceRegistry = new SourceRegistry();
    private final List<ViaProject> projects = new ArrayList<>();

    private boolean cleanup = false;
    private boolean isolatedCache = true;

    public void registerSource(Source<?> project) {
        this.sourceRegistry.register(project);
    }

    public void registerProject(ViaProject project) {
        this.projects.add(project);
    }

    public void reset() {
        this.projects.clear();
        this.sourceRegistry.clear();
    }

    @SuppressWarnings("unchecked")
    public void loadConfig(InputStream input) {
        Map<String, Object> root = new Yaml().load(input);

        this.cleanup = Boolean.TRUE.equals(root.getOrDefault("cleanup", false));
        this.isolatedCache = !Boolean.FALSE.equals(root.getOrDefault("isolated-cache", true));

        Map<String, Object> jenkins = (Map<String, Object>) root.get("jenkins");
        if (jenkins != null) {
            this.registerSource(new JenkinsSource((String) jenkins.get("endpoint")));
        }

        Map<String, Object> github = (Map<String, Object>) root.get("github");
        if (github != null) {
            this.registerSource(new GitHubSource((String) github.get("token"), this.isolatedCache));
        }

        List<Map<String, Object>> projects = (List<Map<String, Object>>) root.get("projects");
        if (projects != null) {
            for (Map<String, Object> projectMap : projects) {
                this.registerProject(ViaProject.create(
                        (String) projectMap.get("name"),
                        (String) projectMap.get("default"),
                        (List<?>) projectMap.get("sources")
                ));
            }
        }
    }

    public List<ViaProject> getProjects() {
        return Collections.unmodifiableList(this.projects);
    }

    public ViaProject getProject(String name) {
        return this.projects.stream()
                .filter(p -> p.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public void updateAll(ProviderContext context) throws UpdateException {
        try {
            List<ViaProject> installed = this.projects.stream()
                    .filter(p -> this.isInstalled(context, p))
                    .toList();

            context.updateStatus(PROGRESS, "Updating " + installed.size() + " installed plugin" + (installed.size() == 1 ? "" : "s") + "...");

            for (ViaProject project : installed) {
                this.updateInternal(context, project, project.defaultSourceId());
            }

            context.updateStatus(SUCCESS, "Successfully updated all installed plugins!");
        } catch (UpdateException e) {
            throw e;
        } finally {
            this.cleanUp(context);
        }
    }

    public void update(ProviderContext context, ViaProject project) throws UpdateException {
        try {
            this.updateInternal(context, project, project.defaultSourceId());
        } finally {
            this.cleanUp(context);
        }
    }

    public void update(
            ProviderContext context,
            ViaProject project,
            String sourceId
    ) throws UpdateException {
        try {
            this.updateInternal(context, project, sourceId);
        } finally {
            this.cleanUp(context);
        }
    }

    private synchronized void updateInternal(
            ProviderContext context,
            ViaProject project,
            String sourceId
    ) throws UpdateException {
        try {
            Path newFile = this.sourceRegistry.provide(context, project, sourceId);
            this.deploy(context, project, newFile);
        } catch (ProviderException e) {
            throw new UpdateException("Failed to update " + project.name(), e);
        }
    }

    private void deploy(ProviderContext context, ViaProject project, Path fileToDeploy) throws UpdateException {
        try {
            Path pluginsDir = context.pluginsDirectory();
            Path oldFile = this.findJar(context, project)
                    .orElseThrow(() -> new UpdateException("Failed to deploy " + project.name() + ": No existing plugin found called " + project.name() + " to replace"));

            // Check if the new file is identical to the old file
            if (this.compare(oldFile, fileToDeploy)) {
                context.updateStatus(SUCCESS, project.name() + " is already up to date");
                return; // No need to update if the files are the same
            }

            // Delete old file
            Files.delete(oldFile);

            // Move file to plugins directory
            Path newFile = pluginsDir.resolve(fileToDeploy.getFileName());
            Files.move(fileToDeploy, newFile, StandardCopyOption.REPLACE_EXISTING);

            String oldName = getDisplayName(oldFile);
            String newName = getDisplayName(newFile);
            context.updateStatus(SUCCESS, "Updated " + oldName + " to " + newName + " successfully");
        } catch (UpdateException e) {
            throw e;
        } catch (IOException e) {
            throw new UpdateException("Failed to deploy " + project.name() + ": " + e.getMessage(), e);
        }
    }

    public void cleanUp(ProviderContext context) {
        Path tmpDirectory = context.tmpDirectory();
        if (!this.cleanup || Files.notExists(tmpDirectory)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(tmpDirectory)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private static @NonNull String getDisplayName(Path file) {
        return file.getFileName().toString()
                .replace(".jar", "")
                .replace("-SNAPSHOT", "");
    }

    public boolean isInstalled(ProviderContext context, ViaProject project) {
        try {
            return this.findJar(context, project).isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    private Optional<Path> findJar(ProviderContext context, ViaProject project) throws IOException {
        if (!Files.exists(context.pluginsDirectory()) || !Files.isDirectory(context.pluginsDirectory())) {
            return Optional.empty();
        }
        try (Stream<Path> stream = Files.list(context.pluginsDirectory())) {
            return stream
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        String prefix = project.name() + "-";
                        return name.endsWith(".jar")
                                && name.startsWith(prefix)
                                && Character.isDigit(name.charAt(prefix.length()));
                    })
                    .findFirst();
        }
    }

    private boolean compare(Path file1, Path file2) throws IOException {
        if (Files.size(file1) != Files.size(file2)) {
            return false;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash1 = digest.digest(Files.readAllBytes(file1));
            digest.reset();
            byte[] hash2 = digest.digest(Files.readAllBytes(file2));
            return Arrays.equals(hash1, hash2);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
