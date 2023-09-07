import { getBookmark } from "@/api/bookmarks";
import { getTopic } from "@/api/topics";
import { TopicDashboard } from "@/components/topicPage";

export default async function AsyncTopicPage({
  params,
}: {
  params: { bookmark: string; topic: string };
}) {
  const bookmark = await getBookmark(params.bookmark);
  const topic = await getTopic(bookmark.attributes.cluster.id, params.topic);
  return <TopicDashboard topic={topic} />;
}
