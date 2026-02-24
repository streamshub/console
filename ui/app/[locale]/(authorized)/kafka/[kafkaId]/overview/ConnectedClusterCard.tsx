import { ConsumerGroupsResponse } from "@/api/groups/schema";
import { ClusterDetail } from "@/api/kafka/schema";
import { ApiResponse } from "@/api/api";
import { ClusterCard } from "@/components/ClusterOverview/ClusterCard";

export async function ConnectedClusterCard({
  cluster,
  groups,
}: {
  cluster: Promise<ClusterDetail | null>;
  groups: Promise<ApiResponse<ConsumerGroupsResponse>>;
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
        groups={undefined}
        brokersOnline={undefined}
        brokersTotal={undefined}
        kafkaId={res?.id}
        managed={res?.meta?.managed || false}
      />
    );
  }
  const groupCount = await groups.then((grpResp) =>
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
      groups={groupCount}
      brokersOnline={brokersOnline}
      brokersTotal={brokersTotal}
      kafkaId={res.id}
      managed={res.meta?.managed || false}
    />
  );
}
