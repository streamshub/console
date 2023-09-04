import { getAuthProfile } from "@/api/auth";
import { getTopic } from "@/api/topics";
import { TopicDashboard } from "@/components/topicPage";

export default async function AsyncTopicPage({
  params,
}: {
  params: { authProfile: string; topic: string };
}) {
  const authProfile = await getAuthProfile(params.authProfile);
  const topic = await getTopic(authProfile.attributes.cluster.id, params.topic);
  return <TopicDashboard topic={topic} />;
}
