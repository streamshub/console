import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
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
    <Suspense fallback={<Header params={{ kafkaId, groupId }} />}>
      <ConnectedAppHeader params={{ kafkaId, groupId }} />
    </Suspense>
  );
}

async function ConnectedAppHeader({
  params: { kafkaId, groupId },
}: {
  params: KafkaConsumerGroupMembersParams;
}) {
  return <Header params={{ kafkaId, groupId }} />;
}

function Header({
  params: { kafkaId, groupId },
}: {
  params: KafkaConsumerGroupMembersParams;
}) {
  const t = useTranslations("ConsumerGroupsTable");

  return (
    <AppHeader
      title={t("reset_consumer_offset")}
      subTitle={
        decodeURIComponent(groupId) === "+" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          <RichText>
            {(tags) => t.rich("consumer_name", { ...tags, groupId })}
          </RichText>
        )
      }
    />
  );
}
