import { getConsumerGroup } from "@/api/groups/actions";
import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/KafkaConsumerGroupMembers.params";
import { AppHeader } from "@/components/AppHeader";
import { Suspense } from "react";
import { useTranslations } from "next-intl";
import RichText from "@/components/RichText";

export default function Page({
  params: { kafkaId, groupId },
}: {
  params: KafkaConsumerGroupMembersParams;
}) {
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

function Header({
  groupIdDisplay,
}: {
  groupIdDisplay: string;
}) {
  const t = useTranslations();

  return (
    <AppHeader
      title={t("GroupsTable.reset_consumer_offset")}
      subTitle={
        groupIdDisplay === "" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          <RichText>
            {(tags) => t.rich("GroupsTable.consumer_name", { ...tags, groupId: groupIdDisplay })}
          </RichText>
        )
      }
    />
  );
}
