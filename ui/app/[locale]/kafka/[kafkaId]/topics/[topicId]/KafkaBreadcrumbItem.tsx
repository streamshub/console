"use client";
import { ClusterDetail, ClusterList } from "@/api/kafka";
import { KafkaSwitcher } from "@/app/[locale]/kafka/[kafkaId]/KafkaSwitcher";
import { Suspense } from "react";

export function KafkaBreadcrumbItem({
  selected,
  clusters,
}: {
  selected: ClusterDetail;
  clusters: ClusterList[];
}) {
  return (
    <Suspense fallback={"Loading clusters..."}>
      <KafkaSwitcher
        selected={selected}
        clusters={clusters}
        isActive={false}
        getSwitchHref={(kafkaId) => `/kafka/${kafkaId}/topics`}
      />
    </Suspense>
  );
}
