import { getConsumerGroup } from "@/api/consumerGroups/actions";
import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { AppHeader } from "@/components/AppHeader";
import { Suspense } from "react";
import { getTranslations } from "next-intl/server";
import RichText from "@/components/RichText";

export default async function Page({
  params: paramsPromise,
}: {
  params: Promise<KafkaConsumerGroupMembersParams>;
}) {
  const { kafkaId, groupId } = await paramsPromise;
  return (
    <Suspense fallback={<Header groupIdDisplay={""} />}>
      <ConnectedAppHeader params={{ kafkaId, groupId }} />
    </Suspense>
  );
}

async function ConnectedAppHeader({
  params: { kafkaId, groupId },
}: {
  params: KafkaConsumerGroupMembersParams;
}) {
  const consumerGroup = (await getConsumerGroup(kafkaId, groupId)).payload;
  let groupIdDisplay = "";

  if (consumerGroup) {
    groupIdDisplay = consumerGroup.attributes.groupId;
  }

  return <Header groupIdDisplay={groupIdDisplay} />;
}

async function Header({
  groupIdDisplay,
}: {
  groupIdDisplay: string;
}) {
  const t = await getTranslations();

  return (
    <AppHeader
      title={t("ConsumerGroupsTable.reset_consumer_offset")}
      subTitle={
        groupIdDisplay === "" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          <RichText>
            {(tags) => t.rich("ConsumerGroupsTable.consumer_name", { ...tags, groupId: groupIdDisplay })}
          </RichText>
        )
      }
    />
  );
}
