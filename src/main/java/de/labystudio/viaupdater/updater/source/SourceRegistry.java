package de.labystudio.viaupdater.updater.source;

import de.labystudio.viaupdater.updater.ViaProject;
import de.labystudio.viaupdater.updater.source.provider.ProviderContext;
import de.labystudio.viaupdater.updater.source.provider.exception.ProviderException;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SourceRegistry {

    private final Map<SourceType, Source<?>> sources = new HashMap<>();

    public void register(Source<?> source) {
        SourceType type = SourceType.fromSource(source);
        this.sources.put(type, source);
    }

    public void clear() {
        this.sources.clear();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Path provide(
            ProviderContext context,
            ViaProject project,
            String sourceId
    ) throws ProviderException {
        SourceProject sourceProject = project.project(sourceId);
        if (sourceProject == null) {
            throw new ProviderException("Source project with id " + sourceId + " is not defined for project " + project.name());
        }

        SourceType type = SourceType.fromProject(sourceProject);
        Source source = this.sources.get(type);
        if (source == null) {
            throw new ProviderException("Source " + type + " is not registered");
        }
        return source.provide(context, project, sourceProject);
    }

}
