import { getBookmark } from "@/api/bookmarks";
import { getTopic } from "@/api/topics";
import { TopicPage } from "@/components/topicPage";
import { PropsWithChildren } from "react";

export default async function AsyncTopicPage({
  children,
  params,
}: PropsWithChildren<{
  params: { bookmark: string; topic: string };
}>) {
  const bookmark = await getBookmark(params.bookmark);
  const topic = await getTopic(bookmark.attributes.cluster.id, params.topic);

  return <TopicPage topic={topic}>{children}</TopicPage>;
}
