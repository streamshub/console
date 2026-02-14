import { getConsumerGroup } from "@/api/consumerGroups/actions";
import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { AppHeader } from "@/components/AppHeader";
import { Suspense } from "react";
import { getTranslations } from "next-intl/server";
import { ConsumerGroupActionButton } from "./ConsumerGroupActionButton";
import RichText from "@/components/RichText";
import { hasPrivilege } from "@/utils/privileges";

export default async function Page({
  params: paramsPromise,
}: {
  params: Promise<KafkaConsumerGroupMembersParams>;
}) {
  const { kafkaId, groupId } = await paramsPromise;
  return (
    <Suspense
      fallback={<Header params={{ kafkaId, groupId, groupIdDisplay: "" }} disabled={true} />}
    >
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
  let disabled = true;
  let groupIdDisplay = "";

  if (consumerGroup) {
    disabled = consumerGroup.attributes.state !== "EMPTY" || !hasPrivilege("UPDATE", consumerGroup);
    groupIdDisplay = consumerGroup.attributes.groupId;
  }

  return <Header params={{ kafkaId, groupId, groupIdDisplay }} disabled={disabled} />;
}

async function Header({
  disabled,
  params: { kafkaId, groupId, groupIdDisplay },
}: {
  disabled: boolean;
  params: { kafkaId: string; groupId: string; groupIdDisplay: string };
}) {
  const t = await getTranslations();

  return (
    <AppHeader
      title={
        groupIdDisplay === "" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          groupIdDisplay
        )
      }
      actions={[
        <ConsumerGroupActionButton
          key={"consumergGroupActionButton"}
          disabled={disabled}
          kafkaId={kafkaId}
          groupId={groupId}
        />,
      ]}
    />
  );
}
