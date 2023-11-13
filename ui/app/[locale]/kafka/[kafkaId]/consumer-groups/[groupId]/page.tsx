import { getConsumerGroup } from "@/api/consumerGroups/actions";
import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { MembersTable } from "@/app/[locale]/kafka/[kafkaId]/consumer-groups/[groupId]/MembersTable";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";
import { Suspense } from "react";

export default function ConsumerGroupMembersPage({
  params: { kafkaId, groupId },
}: {
  params: KafkaConsumerGroupMembersParams;
}) {
  return (
    <PageSection>
      <Suspense
        fallback={<MembersTable kafkaId={kafkaId} consumerGroup={undefined} />}
      >
        <ConnectedMembersTable params={{ kafkaId, groupId }} />
      </Suspense>
    </PageSection>
  );
}

async function ConnectedMembersTable({
  params: { kafkaId, groupId },
}: {
  params: KafkaParams & { groupId: string };
}) {
  async function refresh() {
    "use server";
    const res = await getConsumerGroup(kafkaId, groupId);
    return res;
  }

  const consumerGroup = await getConsumerGroup(kafkaId, groupId);
  if (!consumerGroup) {
    notFound();
  }
  return <MembersTable kafkaId={kafkaId} consumerGroup={consumerGroup} />;
}
