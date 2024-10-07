import {
  Button,
  Modal,
  ModalVariant,
  Popover,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

export function ValidationModal({
  status,
  onConfirm,
  onCancel,
  isModalOpen,
}: {
  status: "approve" | "stop" | "refresh";
  isModalOpen: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const t = useTranslations("Rebalancing");
  return (
    <Modal
      variant={ModalVariant.medium}
      isOpen={isModalOpen}
      onClose={onCancel}
      title={
        status === "approve"
          ? t("approve_rebalance_proposal")
          : status === "stop"
            ? t("stop_rebalance")
            : t("refresh_rebalance")
      }
      help={
        <Popover
          headerContent={
            status === "approve" ? (
              <div>{t("approve")}</div>
            ) : status === "stop" ? (
              <div>{t("stop")}</div>
            ) : (
              <div>{t("refresh")}</div>
            )
          }
          bodyContent={undefined}
        >
          <Button variant="plain" aria-label="Help">
            <HelpIcon />
          </Button>
        </Popover>
      }
      actions={[
        <Button key="confirm" variant="primary" onClick={onConfirm}>
          {t("confirm")}
        </Button>,
        <Button key="cancel" variant="link" onClick={onCancel}>
          {t("cancel")}
        </Button>,
      ]}
    >
      {status === "approve"
        ? t("approve_rebalance_description")
        : status === "stop"
          ? t("stop_rebalance_description")
          : t("refresh_rebalance_description")}
    </Modal>
  );
}
