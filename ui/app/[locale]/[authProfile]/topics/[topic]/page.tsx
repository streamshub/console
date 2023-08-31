import { getTopic, Topic } from "@/api/topics";
import { PageSection, Text, Title } from "@/libs/patternfly/react-core";

export default async function TopicPage({
  params,
}: {
  params: { authProfile: string; topic: string };
}) {
  const topic = await getTopic(params.authProfile, params.topic);
  return <TopicContent topic={topic} />;
}

function TopicContent({ topic }: { topic: Topic }) {
  return (
    <>
      <PageSection variant={"light"}>
        <Text>
          <Title headingLevel={"h1"}>{topic.attributes.name}</Title>
        </Text>
      </PageSection>
      <PageSection isFilled>lorem</PageSection>
    </>
  );
}
