import React from "react";
import { getTopic } from "@/api/topics/actions";
import { KafkaTopicParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/kafkaTopic.params";
import { AppHeader } from "@/components/AppHeader";
import { ManagedTopicLabel } from "@/components/ManagedTopicLabel";
import { PageSection, Skeleton } from "@/libs/patternfly/react-core";
import { ReactNode, Suspense } from "react";
import { TopicsTabs } from "./TopicTabs";

export type TopicHeaderProps = {
  params: KafkaTopicParams;
  showRefresh?: boolean;
};

export function TopicHeader({
  params: { kafkaId, topicId },
  showRefresh,
}: TopicHeaderProps) {
  const portal = <div key={"topic-header-portal"} id={"topic-header-portal"} />;
  return (
    <Suspense
      fallback={
        <AppHeader
          title={<Skeleton width="35%" />}
          showRefresh={showRefresh}
          navigation={
            <PageSection className={"pf-v6-u-px-sm"} type="subnav">
              <TopicsTabs
                kafkaId={kafkaId}
                topicId={topicId}
                isLoading={true}
              />
            </PageSection>
          }
          actions={[portal]}
        />
      }
    >
      <ConnectedTopicHeader
        params={{ kafkaId, topicId }}
        portal={portal}
        showRefresh={showRefresh}
      />
    </Suspense>
  );
}

async function ConnectedTopicHeader({
  params: { kafkaId, topicId },
  showRefresh,
  portal,
}: {
  params: KafkaTopicParams;
  showRefresh?: boolean;
  portal: ReactNode;
}) {
  const response = await getTopic(kafkaId, topicId);

  if (response.errors) {
    return <AppHeader title={`Topic ${topicId}`} />;
  }

  const topic = response.payload;

  return (
    <AppHeader
      title={
        <>
          {topic?.attributes.name}
          {topic?.meta?.managed === true && <ManagedTopicLabel />}
        </>
      }
      showRefresh={showRefresh}
      navigation={
        <PageSection className={"pf-v6-u-px-sm"} type="subnav">
          <TopicsTabs
            kafkaId={kafkaId}
            topicId={topicId}
            numPartitions={topic?.attributes.numPartitions || 0}
            consumerGroupCount={
              topic?.relationships.groups?.data.length ?? 0
            }
            isLoading={false}
          />
        </PageSection>
      }
      actions={[portal]}
    />
  );
}
