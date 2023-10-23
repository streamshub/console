"use client";
import { ClusterDetail, ClusterList } from "@/api/kafka";
import { KafkaSwitcher } from "@/app/[locale]/kafka/[kafkaId]/KafkaSwitcher";
import { useSelectedLayoutSegment } from "next/navigation";
import { Suspense } from "react";

export function KafkaBreadcrumbItem({
  selected,
  clusters,
  isActive,
}: {
  selected: ClusterDetail;
  clusters: ClusterList[];
  isActive: boolean;
}) {
  const segment = useSelectedLayoutSegment();

  return (
    <Suspense fallback={"Loading clusters..."}>
      <KafkaSwitcher
        selected={selected}
        clusters={clusters}
        isActive={isActive}
        getSwitchHref={(kafkaId) => `/kafka/${kafkaId}/${segment || ""}`}
      />
    </Suspense>
  );
}
