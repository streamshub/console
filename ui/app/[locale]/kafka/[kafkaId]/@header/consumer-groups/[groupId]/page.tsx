import { getConsumerGroup } from "@/api/consumerGroups/actions";
import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { AppHeader } from "@/components/AppHeader";
import { Button, Tooltip } from "@/libs/patternfly/react-core";
import { Suspense } from "react";

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
  return (
    <AppHeader
      title={groupId}
      actions={[
        <Tooltip
          key={"reset"}
          content={
            "It is possible to reset the offset only on stopped consumer groups"
          }
        >
          <Button isDisabled={disabled} aria-disabled={disabled} id={"reset"}>
            Reset offset
          </Button>
        </Tooltip>,
        <Tooltip
          key={"delete"}
          content={"It is possible to delete only stopped consumer groups"}
        >
          <Button
            variant={"danger"}
            aria-disabled={disabled}
            isDisabled={disabled}
          >
            Delete
          </Button>
        </Tooltip>,
      ]}
    />
  );
}
