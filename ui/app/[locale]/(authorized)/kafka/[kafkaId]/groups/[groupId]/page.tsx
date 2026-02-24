import { getTranslations } from "next-intl/server";
import { getConsumerGroup } from "@/api/groups/actions";
import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/KafkaConsumerGroupMembers.params";
import { MembersTable } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/MembersTable";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export async function generateMetadata(props: { params: { kafkaId: string, groupId: string} }) {
  const t = await getTranslations();

  return {
    title: `${t("Group.title")} ${props.params.groupId} | ${t("common.title")}`,
  };
}

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
  const response = await getConsumerGroup(kafkaId, groupId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  async function refresh() {
    "use server";
    const res = await getConsumerGroup(kafkaId, groupId);
    return res?.payload ?? null;
  }

  const consumerGroup = response.payload!;
  return <MembersTable kafkaId={kafkaId} consumerGroup={consumerGroup} refresh={refresh}/>;
}
