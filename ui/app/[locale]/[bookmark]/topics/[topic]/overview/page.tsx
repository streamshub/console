import { getBookmark } from "@/api/bookmarks";
import { getTopic } from "@/api/topics";
import { TopicDashboard } from "@/components/topicPage";

export default async function AsyncTopicPage({
  params,
}: {
  params: { bookmark: string; topic: string };
}) {
  const bookmark = await getBookmark(params.bookmark);
  const kafkaId = bookmark.attributes.cluster!.id;
  const topic = await getTopic(kafkaId, params.topic);
  return <TopicDashboard kafkaId={kafkaId} topic={topic} />;
}
