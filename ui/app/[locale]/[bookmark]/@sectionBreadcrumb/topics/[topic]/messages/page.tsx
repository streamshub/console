import { getBookmark } from "@/api/bookmarks";
import { getTopic } from "@/api/topics";
import { BreadcrumbLink } from "@/components/breadcrumbLink";
import { Breadcrumb, BreadcrumbItem } from "@/libs/patternfly/react-core";

export default async function TopicBreadcrumb({
  params,
}: {
  params: { bookmark: string; topic: string };
}) {
  const bookmark = await getBookmark(params.bookmark);
  const topic = await getTopic(bookmark.attributes.cluster.id, params.topic);
  return (
    <Breadcrumb>
      <BreadcrumbLink href={`../`}>Topics</BreadcrumbLink>
      <BreadcrumbItem isActive>{topic.attributes.name}</BreadcrumbItem>
    </Breadcrumb>
  );
}
