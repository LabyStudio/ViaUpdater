package de.labystudio.viaupdater.updater.builder;

import de.labystudio.viaupdater.util.FileUtils;
import de.labystudio.viaupdater.util.ZipUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ProjectBuilder {

    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private final Path zipSourceIn;
    private final Path buildDirectory;
    private final boolean isolatedCache;

    public ProjectBuilder(Path zipSourceIn, Path buildDirectory, boolean isolatedCache) {
        this.zipSourceIn = zipSourceIn;
        this.buildDirectory = buildDirectory;
        this.isolatedCache = isolatedCache;
    }

    public Path build() throws IOException {
        FileUtils.deleteDirectory(this.buildDirectory);
        Files.createDirectories(this.buildDirectory);

        ZipUtils.extractStrippingRoot(this.zipSourceIn, this.buildDirectory);
        this.runGradleBuild(this.buildDirectory);
        return this.findJarOutput(this.buildDirectory);
    }

    private void runGradleBuild(Path directory) throws IOException {
        Path gradlew = directory.resolve(WINDOWS ? "gradlew.bat" : "gradlew");
        if (!WINDOWS) {
            gradlew.toFile().setExecutable(true);
        }

        Path cacheRoot = directory.getParent();
        Path gradleHome = cacheRoot.resolve(".gradle");
        Path mavenLocal = cacheRoot.resolve(".m2/repository");
        Files.createDirectories(gradleHome);
        Files.createDirectories(mavenLocal);

        ProcessBuilder pb = new ProcessBuilder(gradlew.toAbsolutePath().toString(), "publishToMavenLocal")
                .directory(directory.toFile())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.environment().put("JAVA_HOME", System.getProperty("java.home"));

        if (this.isolatedCache) {
            pb.environment().put("GRADLE_USER_HOME", gradleHome.toAbsolutePath().toString());
            pb.environment().put("GRADLE_OPTS", "-Dmaven.repo.local=" + mavenLocal.toAbsolutePath());
        }

        try {
            int exitCode = pb.start().waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Gradle build failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Gradle build was interrupted", e);
        }
    }

    private Path findJarOutput(Path directory) throws IOException {
        Path libsDir = directory.resolve("build/libs");
        try (Stream<Path> stream = Files.list(libsDir)) {
            return stream
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.endsWith(".jar") && !name.contains("sources") && !name.contains("javadoc");
                    })
                    .max(Comparator.comparingLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    }))
                    .orElseThrow(() -> new IOException("No jar found in " + libsDir));
        }
    }
}
