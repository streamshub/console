import { ExternalLink } from "@/components/Navigation/ExternalLink";
import { Button, List, ListItem, Modal, Stack, StackItem, Text } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export function ResetOffsetModal({
  consumerGroupId,
  members
}: {
  consumerGroupId: string;
  members: string[];
}) {

  const t = useTranslations("ConsumerGroupsTable");

  return (
    <Modal
      title={t("consumer_group_must_be_empty")}
      titleIconVariant="warning"
      isOpen={true}
      description={t("consumer_group_must_be_empty_description")}
      actions={[
        <Button key="close" variant="primary">
          {t("close")}
        </Button>,
        <Button key="refresh" variant="secondary">
          {t("refresh")}
        </Button>,
        <Button key="refresh" variant="link">
          {t("cancel")}
        </Button>
      ]}
    >
      <Stack>
        <StackItem>
          <Text>{t("member_list_to_shutdown")}</Text>
        </StackItem>
        <StackItem>
          <List>
            {members.map((member, index) => (
              <ListItem key={index}>{member}</ListItem>
            ))}
          </List>
        </StackItem>
        <StackItem>
          <ExternalLink
            testId={"learn_to_shutdown_members"}
            href={""}
          >
            {t("learn_to_shutdown_members")}
          </ExternalLink>
        </StackItem>
      </Stack>
    </Modal>
  )
}
