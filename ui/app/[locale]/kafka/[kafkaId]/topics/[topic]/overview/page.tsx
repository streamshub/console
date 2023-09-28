import { getResource } from "@/api/resources";
import { getTopic } from "@/api/topics";
import { notFound } from "next/navigation";
import { TopicDashboard } from "./TopicDashboard";

export default async function AsyncTopicPage({
  params,
}: {
  params: { kafkaId: string; topic: string };
}) {
  const cluster = await getResource(params.kafkaId, "kafka");
  if (!cluster || !cluster.attributes.cluster) {
    notFound();
  }
  const kafkaId = cluster.attributes.cluster.id;
  const topic = await getTopic(kafkaId, params.topic);
  return <TopicDashboard kafkaId={kafkaId} topic={topic} />;
}
