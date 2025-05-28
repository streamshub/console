import { getConsumerGroup } from "@/api/consumerGroups/actions";
import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { AppHeader } from "@/components/AppHeader";
import { Suspense } from "react";
import { useTranslations } from "next-intl";
import { ConsumerGroupActionButton } from "./ConsumerGroupActionButton";
import RichText from "@/components/RichText";

export default function Page({
  params: { kafkaId, groupId },
}: {
  params: KafkaConsumerGroupMembersParams;
}) {
  return (
    <Suspense
      fallback={<Header params={{ kafkaId, groupId }} disabled={true} />}
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
  const disabled =
    (await getConsumerGroup(kafkaId, groupId))?.payload?.attributes.state !==
    "EMPTY";

  return <Header params={{ kafkaId, groupId }} disabled={disabled} />;
}

function Header({
  disabled,
  params: { kafkaId, groupId },
}: {
  disabled: boolean;
  params: KafkaConsumerGroupMembersParams;
}) {
  const t = useTranslations();

  return (
    <AppHeader
      title={
        decodeURIComponent(groupId) === "+" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          groupId
        )
      }
      actions={[
        <ConsumerGroupActionButton
          key={"consumergGroupActionButton"}
          disabled={disabled}
          kafkaId={kafkaId}
          consumerGroupName={groupId}
        />,
      ]}
    />
  );
}
