import { ConsumerGroupsResponse } from "@/api/consumerGroups/schema";
import { ClusterDetail, ClusterKpis } from "@/api/kafka/schema";
import { ClusterCard } from "@/components/ClusterOverview/ClusterCard";

export async function ConnectedClusterCard({
  data,
  consumerGroups,
}: {
  data: Promise<{ cluster: ClusterDetail; kpis: ClusterKpis | null } | null>;
  consumerGroups: Promise<ConsumerGroupsResponse | null>;
}) {
  const res = await data;
  if (!res?.kpis) {
    return (
      <ClusterCard
        isLoading={false}
        status={res?.cluster.attributes.status ?? "n/a"}
        messages={[]}
        name={res?.cluster.attributes.name ?? "n/a"}
        consumerGroups={undefined}
        brokersOnline={undefined}
        brokersTotal={undefined}
        kafkaVersion={res?.cluster.attributes.kafkaVersion ?? "n/a"}
        kafkaId={res?.cluster.id}
      />
    );
  }
  const groupCount = await consumerGroups.then(
    (grpResp) => grpResp?.meta.page.total ?? 0,
  );
  const brokersTotal = Object.keys(res?.kpis.broker_state ?? {}).length;
  const brokersOnline =
    Object.values(res?.kpis.broker_state ?? {}).filter((s) => s === 3).length ||
    0;
  const messages = res?.cluster.attributes.conditions
    ?.filter((c) => "Ready" !== c.type)
    .map((c) => ({
      variant:
        c.type === "Error" ? "danger" : ("warning" as "danger" | "warning"),
      subject: {
        type: c.type!,
        name: res?.cluster.attributes.name ?? "",
        id: res?.cluster.id ?? "",
      },
      message: c.message ?? "",
      date: c.lastTransitionTime ?? "",
    }));

  return (
    <ClusterCard
      isLoading={false}
      status={res?.cluster.attributes.status ?? "n/a"}
      messages={messages ?? []}
      name={res?.cluster.attributes.name || "n/a"}
      consumerGroups={groupCount}
      brokersOnline={brokersOnline}
      brokersTotal={brokersTotal}
      kafkaVersion={res?.cluster.attributes.kafkaVersion ?? "n/a"}
      kafkaId={res.cluster.id}
    />
  );
}
