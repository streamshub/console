import { ConsumerGroupsResponse } from "@/api/consumerGroups/schema";
import { ClusterDetail } from "@/api/kafka/schema";
import { ApiResponse } from "@/api/api";
import { ClusterCard } from "@/components/ClusterOverview/ClusterCard";

export async function ConnectedClusterCard({
  cluster,
  consumerGroups,
}: {
  cluster: Promise<ClusterDetail | null>;
  consumerGroups: Promise<ApiResponse<ConsumerGroupsResponse>>;
}) {
  const res = await cluster;

  const messages = res?.attributes.conditions
    ?.filter((c) => "Ready" !== c.type)
    .map((c) => ({
      variant:
        c.type === "Error" ? "danger" : ("warning" as "danger" | "warning"),
      subject: {
        type: c.type!,
        name: res?.attributes.name ?? "",
        id: res?.id ?? "",
      },
      message: c.message ?? "",
      date: c.lastTransitionTime ?? "",
    }));

  if (!res?.attributes?.metrics) {
    return (
      <ClusterCard
        isLoading={false}
        kafkaDetail={res}
        messages={messages ?? []}
        consumerGroups={undefined}
        brokersOnline={undefined}
        brokersTotal={undefined}
        kafkaId={res?.id}
        managed={res?.meta?.managed || false}
      />
    );
  }
  const groupCount = await consumerGroups.then((grpResp) =>
    grpResp.errors ? undefined : (grpResp.payload?.meta.page.total ?? 0),
  );

  const brokerStatuses = res?.relationships.nodes?.meta?.summary?.statuses?.brokers || {};
  const brokersTotal = Object.values(brokerStatuses).reduce((sum, count) => sum + count, 0);
  const brokersOnline = brokerStatuses["Running"] ?? 0;

  return (
    <ClusterCard
      isLoading={false}
      kafkaDetail={res}
      messages={messages ?? []}
      consumerGroups={groupCount}
      brokersOnline={brokersOnline}
      brokersTotal={brokersTotal}
      kafkaId={res.id}
      managed={res.meta?.managed || false}
    />
  );
}
