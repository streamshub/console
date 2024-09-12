import { getConsumerGroup } from "@/api/consumerGroups/actions";
import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { ResetConsumerOffset } from "./ResetConsumerOffset";

export default function ResetOffsetPage({
  params: { kafkaId, groupId },
}: {
  params: KafkaConsumerGroupMembersParams;
}) {
  return (
    <PageSection>
      <Suspense
        fallback={
          <ResetConsumerOffset
            kafkaId={kafkaId}
            consumerGroupName={groupId}
            topics={[]}
            partitions={[]}
            baseurl={`/kafka/${kafkaId}/consumer-groups/${groupId}`}
          />
        }
      >
        <ConnectedResetOffset params={{ kafkaId, groupId }} />
      </Suspense>
    </PageSection>
  );
}

async function ConnectedResetOffset({
  params: { kafkaId, groupId },
}: {
  params: KafkaParams & { groupId: string };
}) {
  const consumerGroup = await getConsumerGroup(kafkaId, groupId);
  if (!consumerGroup) {
    notFound();
  }

  const topics =
    consumerGroup.attributes.offsets?.map((o) => ({
      topicId: o.topicId,
      topicName: o.topicName,
      partition: o.partition,
    })) || [];

  const topicDetails = topics.map((topic) => ({
    topicId: topic.topicId,
    topicName: topic.topicName,
  }));
  const partitions = topics.map((t) => t.partition);

  return (
    <ResetConsumerOffset
      consumerGroupName={consumerGroup.id}
      topics={topicDetails}
      partitions={partitions}
      baseurl={`/kafka/${kafkaId}/consumer-groups`}
      kafkaId={kafkaId}
    />
  );
}
