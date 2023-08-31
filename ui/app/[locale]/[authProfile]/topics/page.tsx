import { getTopics, Topic } from "@/api/topics";
import { Topics } from "@/components/topics";
import { PageSection, Title } from "@/libs/patternfly/react-core";
import { getUser } from "@/utils/session";

export default async function TopicsPage({
  params,
}: {
  params: { authProfile: string };
}) {
  const auth = await getUser();
  const topics = await getTopics(params.authProfile);
  console.log(topics);
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
