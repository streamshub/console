import { getTopics, TopicList } from "@/api/topics";
import { TopicsTable } from "@/app/[locale]/kafka/[kafkaId]/(root)/topics/TopicsTable";
import { PageSection } from "@/libs/patternfly/react-core";
import { getUser } from "@/utils/session";

export default async function TopicsPage({
  params,
}: {
  params: { kafkaId: string };
}) {
  const auth = await getUser();
  const topics = await getTopics(params.kafkaId);
  return (
    <TopicsContent canCreate={auth.username === "admin"} topics={topics} />
  );
}

function TopicsContent({
  topics,
  canCreate,
}: {
  topics: TopicList[];
  canCreate: boolean;
}) {
  return (
    <PageSection isFilled>
      <TopicsTable topics={topics} canCreate={canCreate} />
    </PageSection>
  );
}
