package de.labystudio.viaupdater.api.github.model.artifacts;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record Artifacts(
        @SerializedName("total_count") Long totalCount,
        List<Artifact> artifacts
) {
}
