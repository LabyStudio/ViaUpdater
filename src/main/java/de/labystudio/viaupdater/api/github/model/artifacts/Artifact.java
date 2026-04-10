package de.labystudio.viaupdater.api.github.model.artifacts;

import com.google.gson.annotations.SerializedName;

public record Artifact(
        Long id,
        @SerializedName("node_id") String nodeId,
        String name,
        @SerializedName("size_in_bytes") Long sizeInBytes,
        String url,
        @SerializedName("archive_download_url") String archiveDownloadUrl,
        Boolean expired,
        @SerializedName("created_at") String createdAt,
        @SerializedName("updated_at") String updatedAt,
        @SerializedName("expires_at") String expiresAt
) {
}
