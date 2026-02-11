import { getConsumerGroup } from "@/api/groups/actions";
import { GroupParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/Group.params";
import { AppHeader } from "@/components/AppHeader";
import { Suspense } from "react";
import { getTranslations } from "next-intl/server";
import RichText from "@/components/RichText";

export default async function Page({
  params: paramsPromise,
}: {
  params: Promise<GroupParams>;
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
  params: GroupParams;
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
