import { getConsumerGroup } from "@/api/groups/actions";
import { GroupParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/Group.params";
import { AppHeader } from "@/components/AppHeader";
import { Suspense } from "react";
import { PageSection, Skeleton } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { ConsumerGroupActionButton } from "./ConsumerGroupActionButton";
import RichText from "@/components/RichText";
import { resetButtonDisabled } from "@/utils/groups";
import { hasPrivilege } from "@/utils/privileges";
import { GroupTabs } from "./GroupTabs";

export function GroupHeader({
  params: { kafkaId, groupId },
}: {
  params: GroupParams;
}) {
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
  params: GroupParams;
}) {
  const group = (await getConsumerGroup(kafkaId, groupId)).payload;
  let disabled = true;
  let groupIdDisplay = "";

  if (group) {
    disabled = resetButtonDisabled(group) || !hasPrivilege("UPDATE", group);
    groupIdDisplay = group.attributes.groupId;
  }

  return <Header params={{ kafkaId, groupId, groupIdDisplay }} disabled={disabled} />;
}

function Header({
  disabled,
  params: { kafkaId, groupId, groupIdDisplay },
}: {
  disabled: boolean;
  params: { kafkaId: string; groupId: string; groupIdDisplay: string };
}) {
  const t = useTranslations();

  return (
    <AppHeader
      title={
        groupIdDisplay === "" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          groupIdDisplay
        )
      }
      navigation={
       <PageSection className={"pf-v6-u-px-sm"} type="subnav">
         <GroupTabs
           kafkaId={kafkaId}
           groupId={groupId}
         />
       </PageSection>
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
