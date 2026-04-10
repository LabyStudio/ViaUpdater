package de.labystudio.viaupdater.updater;

import de.labystudio.viaupdater.updater.source.SourceProject;
import de.labystudio.viaupdater.updater.source.SourceType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ViaProject(
        String name,
        String defaultSourceId,
        Map<String, SourceProject> sources
) {

    public ViaProject addSource(String id, SourceProject project) {
        this.sources.put(id, project);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends SourceProject> T project(String id) {
        return (T) this.sources.get(id);
    }

    public static ViaProject create(String name, String defaultSourceId) {
        return new ViaProject(
                name,
                defaultSourceId,
                new HashMap<>()
        );
    }

    public static ViaProject create(
            String name,
            String defaultSourceId,
            List<?> sourceList
    ) {
        Map<String, SourceProject> sourceMap = new HashMap<>();
        for (Object entry : sourceList) {
            Map<?, ?> map = (Map<?, ?>) entry;
            String id = (String) map.get("id");
            SourceType type = SourceType.valueOf((String) map.get("type"));
            sourceMap.put(id, type.createProject(map));
        }
        return new ViaProject(
                name,
                defaultSourceId,
                sourceMap
        );
    }

}
