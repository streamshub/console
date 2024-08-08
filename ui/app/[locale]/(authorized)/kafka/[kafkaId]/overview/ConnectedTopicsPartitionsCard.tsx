import { ClusterDetail, ClusterKpis } from "@/api/kafka/schema";
import { TopicsPartitionsCard } from "@/components/ClusterOverview/TopicsPartitionsCard";

export async function ConnectedTopicsPartitionsCard({
  data,
}: {
  data: Promise<{ cluster: ClusterDetail; kpis: ClusterKpis | null } | null>;
}) {
  const res = await data;
  if (!res?.kpis) {
    return null;
  }
  const topicsTotal = res?.kpis.total_topics || 0;
  const topicsUnderreplicated = res?.kpis.underreplicated_topics || 0;
  return (
    <TopicsPartitionsCard
      isLoading={false}
      partitions={Math.max(0, res?.kpis.total_partitions || 0)}
      topicsReplicated={Math.max(0, topicsTotal - topicsUnderreplicated)}
      topicsTotal={Math.max(0, topicsTotal)}
      topicsUnderReplicated={Math.max(0, topicsUnderreplicated)}
    />
  );
}
