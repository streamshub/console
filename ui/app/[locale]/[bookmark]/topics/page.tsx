import { getBookmark } from "@/api/bookmarks";
import { getTopics } from "@/api/topics";
import { Topic } from "@/api/types";
import { Topics } from "@/components/topics";
import { PageSection, Title } from "@/libs/patternfly/react-core";
import { getUser } from "@/utils/session";

export default async function TopicsPage({
  params,
}: {
  params: { bookmark: string };
}) {
  const auth = await getUser();
  const bookmark = await getBookmark(params.bookmark);
  const topics = await getTopics(bookmark.attributes.cluster.id);
  return (
    <TopicsContent canCreate={auth.username === "admin"} topics={topics} />
  );
}

function TopicsContent({
  topics,
  canCreate,
}: {
  topics: Topic[];
  canCreate: boolean;
}) {
  return (
    <>
      <PageSection variant={"light"}>
        <Title headingLevel={"h1"}>All topics</Title>
      </PageSection>
      <PageSection isFilled>
        <Topics topics={topics} canCreate={canCreate} />
      </PageSection>
    </>
  );
}
