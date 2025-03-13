package com.github.streamshub.console.api.support;

import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.admin.DescribeMetadataQuorumResult;
import org.apache.kafka.clients.admin.QuorumInfo;
import org.apache.kafka.common.errors.UnsupportedVersionException;

public class MetadataQuorumSupport {

    private MetadataQuorumSupport() {
    }

    /**
     * Converts a {@code DescribeMetadataQuorumResult} to
     * {@code CompletableFuture<QuorumInfo>}. If the describe metadata quorum
     * operation is not supported, the future is completed with null. Other errors
     * are propagated through the returned future.
     */
    public static CompletableFuture<QuorumInfo> quorumInfo(DescribeMetadataQuorumResult result) {
        CompletableFuture<QuorumInfo> promise = new CompletableFuture<>();

        result.quorumInfo().whenComplete((quorumInfo, error) -> {
            if (error instanceof UnsupportedVersionException) {
                promise.complete(null);
            } else if (error != null) {
                promise.completeExceptionally(error);
            } else {
                promise.complete(quorumInfo);
            }
        });

        return promise;
    }

}
