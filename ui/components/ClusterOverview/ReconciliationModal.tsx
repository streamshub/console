"use client";

import { Button, Modal, ModalVariant } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export function ReconciliationModal({
  isModalOpen,
  onClickClose,
  onClickPauseReconciliation,
}: {
  isModalOpen: boolean;
  onClickClose: () => void;
  onClickPauseReconciliation: () => void;
}) {
  const t = useTranslations();
  return (
    <Modal
      title={t("reconciliation.pause_reconciliation")}
      isOpen={isModalOpen}
      variant={ModalVariant.medium}
      onClose={onClickClose}
      actions={[
        <Button
          key="confirm"
          variant="primary"
          onClick={onClickPauseReconciliation}
        >
          {t("reconciliation.confirm")}
        </Button>,
        <Button key="cancel" variant="link" onClick={onClickClose}>
          {t("reconciliation.cancel")}
        </Button>,
      ]}
    >
      {t("reconciliation.pause_reconciliation_description")}
    </Modal>
  );
}
