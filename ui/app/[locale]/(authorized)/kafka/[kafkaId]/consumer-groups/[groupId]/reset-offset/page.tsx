import { getTranslations } from "next-intl/server";
import { getConsumerGroup } from "@/api/consumerGroups/actions";
import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import { ResetConsumerOffset } from "./ResetConsumerOffset";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export async function generateMetadata(props: { params: Promise<{ kafkaId: string, groupId: string}> }) {
  const t = await getTranslations();

  return {
    title: `${t("ConsumerGroupsTable.reset_offset")} ${(await props.params).groupId} | ${t("common.title")}`,
  };
}

export default async function ResetOffsetPage(
  props: {
    params: Promise<KafkaConsumerGroupMembersParams>;
  }
) {
  const params = await props.params;

  const {
    kafkaId,
    groupId
  } = params;

  return (
    <PageSection>
      <Suspense
        fallback={
          <ResetConsumerOffset
            kafkaId={kafkaId}
            consumerGroup={{
              id: groupId,
              type: "consumerGroups",
              attributes: {
                groupId: "-",
                state: "UNKNOWN",
              }
            }}
            topics={[]}
            partitions={[]}
            baseurl={`/kafka/${kafkaId}/consumer-groups`}
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
  const response = await getConsumerGroup(kafkaId, groupId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const consumerGroup = response.payload!;

  const topics =
    consumerGroup.attributes.offsets?.map((o) => ({
      topicId: o.topicId,
      topicName: o.topicName,
      partition: o.partition,
    })) ?? [];

  const undescribedTopics = topics
    .filter((topic) => topic.topicId === undefined)
    .map((topic) => topic.topicName);

  if (undescribedTopics.length > 0) {
    const distinct = new Set(undescribedTopics);
    return <NoDataErrorState errors={[{
        title: "Insufficient access",
        detail: "Missing required access to topics: " + Array.from(distinct).join(", ")
    }]} />;
  }

  const topicDetails = topics.map((topic) => ({
    topicId: topic.topicId!,
    topicName: topic.topicName,
  }));

  const partitions = topics.map((t) => ({
    topicId: t.topicId!,
    partitionNumber: t.partition
  }));

  return (
    <ResetConsumerOffset
      consumerGroup={consumerGroup}
      topics={topicDetails}
      partitions={partitions}
      baseurl={`/kafka/${kafkaId}/consumer-groups`}
      kafkaId={kafkaId}
    />
  );
}
