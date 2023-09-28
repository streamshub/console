import { getResource } from "@/api/resources";
import { getTopics } from "@/api/topics";
import { Topic } from "@/api/types";
import { TopicsTable } from "@/app/[locale]/kafka/[kafkaId]/(root)/topics/TopicsTable";
import { PageSection } from "@/libs/patternfly/react-core";
import { getUser } from "@/utils/session";
import { notFound } from "next/navigation";

export default async function TopicsPage({
  params,
}: {
  params: { kafkaId: string };
}) {
  const auth = await getUser();
  const cluster = await getResource(params.kafkaId, "kafka");
  if (!cluster || !cluster.attributes.cluster) {
    notFound();
  }
  const topics = await getTopics(cluster.attributes.cluster.id);
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
    <PageSection isFilled>
      <TopicsTable topics={topics} canCreate={canCreate} />
    </PageSection>
  );
}
