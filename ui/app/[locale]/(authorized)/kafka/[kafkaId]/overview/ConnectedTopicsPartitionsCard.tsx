import { ApiResponse } from "@/api/api";
import { TopicsResponse } from "@/api/topics/schema";
import { TopicsPartitionsCard } from "@/components/ClusterOverview/TopicsPartitionsCard";

export async function ConnectedTopicsPartitionsCard({
  data,
}: {
  data: Promise<ApiResponse<TopicsResponse>>;
}) {
  const summary = (await data).payload?.meta?.summary;
  const totalPartitions = summary?.totalPartitions ?? 0;
  const totalReplicated = summary?.statuses.FullyReplicated ?? 0;
  const totalUnderReplicated = (summary?.statuses.UnderReplicated ?? 0) + (summary?.statuses.PartiallyOffline ?? 0);
  const totalOffline = summary?.statuses.Offline ?? 0;
  const totalUnknown = summary?.statuses.Unknown ?? 0;
  const errors = (await data).errors;

  return (
    <TopicsPartitionsCard
      isLoading={false}
      partitions={totalPartitions}
      topicsReplicated={totalReplicated}
      topicsUnderReplicated={totalUnderReplicated}
      topicsOffline={totalOffline}
      topicsUnknown={totalUnknown}
      errors={errors}
    />
  );
}
