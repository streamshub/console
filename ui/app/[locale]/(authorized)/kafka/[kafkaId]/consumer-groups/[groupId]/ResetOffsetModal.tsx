import { ExternalLink } from "@/components/Navigation/ExternalLink";
import {
  Button,
  List,
  ListItem,
  Modal,
  ModalVariant,
  Stack,
  StackItem,
  Text,
} from "@/libs/patternfly/react-core";
import { isProductizedBuild } from "@/utils/env";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";

export function ResetOffsetModal({
  members,
  isResetOffsetModalOpen,
  onClickClose,
  kafkaId,
  consumerGroupName,
}: {
  members: string[];
  isResetOffsetModalOpen: boolean;
  onClickClose: () => void;
  kafkaId: string;
  consumerGroupName: string;
}) {
  const t = useTranslations("ConsumerGroupsTable");
  const router = useRouter();

  const refresh = () => {
    if (members.length === 0) {
      router.push(
        `/kafka/${kafkaId}/consumer-groups/${consumerGroupName}/reset-offset`,
      );
    }
  };

  return (
    <Modal
      title={t("consumer_group_must_be_empty")}
      titleIconVariant="warning"
      isOpen={isResetOffsetModalOpen}
      variant={ModalVariant.medium}
      onClose={onClickClose}
      actions={[
        <Button key="close" variant="primary" onClick={onClickClose}>
          {t("close")}
        </Button>,
        <Button key="refresh" variant="secondary" onClick={refresh}>
          {t("refresh")}
        </Button>,
        <Button key="refresh" variant="link" onClick={onClickClose}>
          {t("cancel")}
        </Button>,
      ]}
    >
      <Stack hasGutter>
        <StackItem>
          <Text>{t("member_shutdown_helper_text")}</Text>
        </StackItem>
        <StackItem>
          <List>
            {members.map((member, index) => (
              <ListItem key={index}>{member}</ListItem>
            ))}
          </List>
        </StackItem>
        <StackItem>
          <Text>{t("shutdown_active_members")}</Text>
        </StackItem>
        {isProductizedBuild && (
          <StackItem>
            <ExternalLink testId={"learn_to_shutdown_members"} href={"https://docs.redhat.com/en/documentation/red_hat_streams_for_apache_kafka/2.7/html-single/kafka_configuration_tuning/index#managing_offset_policy"}>
              {t("learn_to_shutdown_members")}
            </ExternalLink>
          </StackItem>
        )}
      </Stack>
    </Modal>
  );
}
