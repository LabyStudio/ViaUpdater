package de.labystudio.viaupdater.api.github.model.runs;

import com.google.gson.annotations.SerializedName;

public record HeadCommit(
        String id,
        @SerializedName("tree_id") String treeId,
        String message,
        String timestamp,
        Author author,
        Committer committer
) {
}
