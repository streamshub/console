import { getConsumerGroup } from "@/api/consumerGroups/actions";
import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { AppHeader } from "@/components/AppHeader";
import { Suspense } from "react";
import { useTranslations } from "next-intl";
import { ConsumerGroupActionButton } from "./ConsumerGroupActionButton";

export const fetchCache = "force-cache";

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
  const cg = await getConsumerGroup(kafkaId, groupId);
  const disabled = cg.attributes.state !== "EMPTY";
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
      title={decodeURIComponent(groupId) === "+" ? <i>Empty Name</i> : groupId}
      actions={[<ConsumerGroupActionButton
        key={"consumergGroupActionButton"}
        disabled={disabled}
        kafkaId={kafkaId}
        consumerGroupName={groupId} />
      ]}
    />
  );
}
