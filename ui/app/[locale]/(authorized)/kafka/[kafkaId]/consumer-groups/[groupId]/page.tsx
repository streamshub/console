import { getTranslations } from "next-intl/server";
import { getConsumerGroup } from "@/api/consumerGroups/actions";
import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { MembersTable } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/MembersTable";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export async function generateMetadata(props: { params: Promise<{ kafkaId: string, groupId: string}> }) {
  const t = await getTranslations();

  return {
    title: `${t("ConsumerGroup.title")} ${(await props.params).groupId} | ${t("common.title")}`,
  };
}

export default async function ConsumerGroupMembersPage(
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
