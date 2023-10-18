import { getTopics, TopicList } from "@/api/topics";
import { TopicsTable } from "@/app/[locale]/kafka/[kafkaId]/topics/TopicsTable";
import { PageSection } from "@/libs/patternfly/react-core";

export default async function TopicsPage({
  params,
}: {
  params: { kafkaId: string };
  searchParams: {
    limit: string | undefined;
    sort: string | undefined;
    "filter[offset]": string | undefined;
    "filter[timestamp]": string | undefined;
    "filter[epoch]": string | undefined;
  };
}) {
  const topics = await getTopics(params.kafkaId);
  return (
    <TopicsContent
      canCreate={process.env.CONSOLE_MODE === "read-write"}
      topics={topics}
    />
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
