import { getAuthProfile } from "@/api/auth";
import { getTopic } from "@/api/topics";
import { TopicPage } from "@/components/topicPage";
import { PropsWithChildren } from "react";

export default async function AsyncTopicPage({
  children,
  params,
}: PropsWithChildren<{
  params: { authProfile: string; topic: string };
}>) {
  const authProfile = await getAuthProfile(params.authProfile);
  const topic = await getTopic(authProfile.attributes.cluster.id, params.topic);

  return <TopicPage topic={topic}>{children}</TopicPage>;
}
