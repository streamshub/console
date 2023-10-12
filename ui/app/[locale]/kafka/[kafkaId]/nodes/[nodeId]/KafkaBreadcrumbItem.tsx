"use client";
import { ClusterDetail } from "@/api/kafka";
import { KafkaResource } from "@/api/resources";
import { KafkaSwitcher } from "@/app/[locale]/kafka/[kafkaId]/KafkaSwitcher";
import { Suspense } from "react";

export function KafkaBreadcrumbItem({
  selected,
  clusters,
}: {
  selected: ClusterDetail;
  clusters: KafkaResource[];
}) {
  return (
    <Suspense fallback={"Loading clusters..."}>
      <KafkaSwitcher
        selected={selected}
        clusters={clusters}
        isActive={false}
        getSwitchHref={(kafkaId) => `/kafka/${kafkaId}/nodes`}
      />
    </Suspense>
  );
}
