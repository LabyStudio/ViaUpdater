package de.labystudio.viaupdater.updater.source;

import de.labystudio.viaupdater.api.github.model.GitHubProject;
import de.labystudio.viaupdater.api.jenkins.model.JenkinsProject;
import de.labystudio.viaupdater.updater.source.sources.GitHubSource;
import de.labystudio.viaupdater.updater.source.sources.JenkinsSource;

import java.lang.reflect.RecordComponent;
import java.util.Map;

public enum SourceType {
    JENKINS(JenkinsProject.class, JenkinsSource.class),
    GITHUB(GitHubProject.class, GitHubSource.class);

    private final Class<? extends SourceProject> projectClass;
    private final Class<? extends Source> sourceClass;

    SourceType(
            Class<? extends SourceProject> projectClass,
            Class<? extends Source> sourceClass
    ) {
        this.projectClass = projectClass;
        this.sourceClass = sourceClass;
    }

    public Class<? extends SourceProject> projectClass() {
        return this.projectClass;
    }

    public Class<? extends Source> sourceClass() {
        return this.sourceClass;
    }

    public SourceProject createProject(Map<?, ?> map) {
        RecordComponent[] components = this.projectClass.getRecordComponents();
        Object[] args = new Object[components.length];
        Class<?>[] types = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) {
            args[i] = map.get(components[i].getName());
            types[i] = components[i].getType();
        }
        try {
            return this.projectClass.getDeclaredConstructor(types).newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create " + this.projectClass.getSimpleName(), e);
        }
    }

    public static SourceType fromProject(SourceProject project) {
        for (SourceType type : SourceType.values()) {
            if (type.projectClass.equals(project.getClass())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown project type: " + project.getClass().getSimpleName());
    }

    public static SourceType fromSource(Source source) {
        for (SourceType type : SourceType.values()) {
            if (type.sourceClass.equals(source.getClass())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown source type: " + source.getClass().getSimpleName());
    }
}
