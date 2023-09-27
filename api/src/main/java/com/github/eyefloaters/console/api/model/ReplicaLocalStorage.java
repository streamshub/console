package com.github.eyefloaters.console.api.model;

import org.apache.kafka.clients.admin.ReplicaInfo;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ReplicaLocalStorage(
        @Schema(readOnly = true, description = "The total size of the log segments local to the replica, in bytes.")
        long size,

        @Schema(readOnly = true, description = """
                The lag of the log's log end offset with respect to the partition's high watermark
                (if it is the current log for the partition), or the current replica's log end offset
                (if it is the `future` log for the partition).
                """)
        long offsetLag,

        @Schema(readOnly = true, description = """
                false this replica is the active log being used by the broker
                true if this replica represents a log being moved between log dirs on the broker  
                """)
        boolean future) {

    public static ReplicaLocalStorage fromKafkaModel(ReplicaInfo info) {
        return new ReplicaLocalStorage(info.size(), info.offsetLag(), info.isFuture());
    }
}
