package de.labystudio.viaupdater.api.github.model.runs;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record Run(
        Long id,
        String name,
        @SerializedName("node_id") String nodeId,
        @SerializedName("head_branch") String headBranch,
        @SerializedName("head_sha") String headSha,
        @SerializedName("run_number") Long runNumber,
        String event,
        String status,
        String conclusion,
        @SerializedName("workflow_id") Long workflowId,
        @SerializedName("check_suite_id") Long checkSuiteId,
        @SerializedName("check_suite_node_id") String checkSuiteNodeId,
        String url,
        @SerializedName("html_url") String htmlUrl,
        @SerializedName("pull_requests") List<Object> pullRequests,
        @SerializedName("created_at") String createdAt,
        @SerializedName("updated_at") String updatedAt,
        @SerializedName("run_attempt") Long runAttempt,
        @SerializedName("run_started_at") String runStartedAt,
        @SerializedName("jobs_url") String jobsUrl,
        @SerializedName("logs_url") String logsUrl,
        @SerializedName("check_suite_url") String checkSuiteUrl,
        @SerializedName("artifacts_url") String artifactsUrl,
        @SerializedName("cancel_url") String cancelUrl,
        @SerializedName("rerun_url") String rerunUrl,
        @SerializedName("previous_attempt_url") Object previousAttemptUrl,
        @SerializedName("workflow_url") String workflowUrl,
        @SerializedName("head_commit") HeadCommit headCommit,
        Repository repository,
        @SerializedName("head_repository") HeadRepository headRepository
) {
}
