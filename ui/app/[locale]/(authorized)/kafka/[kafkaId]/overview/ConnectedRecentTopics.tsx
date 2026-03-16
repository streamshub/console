import { ViewedTopic } from "@/api/topics/actions";
import { RecentTopicsCard } from "@/components/ClusterOverview/RecentTopicsCard";

export async function ConnectedRecentTopics({ data }: { data: ViewedTopic[] }) {
  const viewedTopics = data;
  return <RecentTopicsCard viewedTopics={viewedTopics} isLoading={false} />;
}
