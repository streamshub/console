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
        status={res?.attributes.status ?? "n/a"}
        messages={messages ?? []}
        name={res?.attributes.name ?? "n/a"}
        consumerGroups={undefined}
        brokersOnline={undefined}
        brokersTotal={undefined}
        kafkaVersion={res?.attributes.kafkaVersion ?? "n/a"}
        kafkaId={res?.id}
        managed={res?.meta?.managed || false}
      />
    );
  }
  const groupCount = await consumerGroups.then((grpResp) =>
    grpResp.errors ? undefined : (grpResp.payload?.meta.page.total ?? 0),
  );

  const brokersTotal = res?.relationships.nodes?.meta?.summary?.brokersCount ?? 0;
  const brokersOnline = res?.relationships.nodes?.meta?.summary?.brokersRunning ?? 0;

  return (
    <ClusterCard
      isLoading={false}
      status={
        res?.attributes.status ??
        (brokersOnline == brokersTotal ? "Ready" : "Not Available")
      }
      messages={messages ?? []}
      name={res?.attributes.name ?? "n/a"}
      consumerGroups={groupCount}
      brokersOnline={brokersOnline}
      brokersTotal={brokersTotal}
      kafkaVersion={res?.attributes.kafkaVersion ?? "Not Available"}
      kafkaId={res.id}
      managed={res.meta?.managed || false}
    />
  );
}
