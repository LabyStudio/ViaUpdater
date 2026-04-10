package de.labystudio.viaupdater.api.github.model.runs;

import com.google.gson.annotations.SerializedName;

public record WorkflowRuns(
        @SerializedName("total_count") Long totalCount,
        @SerializedName("workflow_runs") Run[] workflowRuns
) {
}
