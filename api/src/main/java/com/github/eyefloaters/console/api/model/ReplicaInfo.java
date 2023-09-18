package com.github.eyefloaters.console.api.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ReplicaInfo(
        @Schema(readOnly = true, description = "The total size of the log segments in this replica in bytes.")
        long size,

        @Schema(readOnly = true, description = """
                The lag of the log's LEO with respect to the partition's high watermark
                (if it is the current log for the partition) or the current replica's LEO
                (if it is the `future` log for the partition).
                """)
        long offsetLag,

        @Schema(readOnly = true, description = """
                Whether this replica has been created by a AlterReplicaLogDirsRequest
                but not yet replaced the current replica on the broker
                """)
        boolean future) {

    public static ReplicaInfo fromKafkaModel(org.apache.kafka.clients.admin.ReplicaInfo info) {
        return new ReplicaInfo(info.size(), info.offsetLag(), info.isFuture());
    }
}
