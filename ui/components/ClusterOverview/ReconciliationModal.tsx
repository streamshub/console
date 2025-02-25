"use client";

import { Button, Modal, ModalVariant } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export function ReconciliationModal({
  isModalOpen,
  onClickClose,
  onClickPauseReconciliation,
  isReconciliationPaused,
}: {
  isModalOpen: boolean;
  onClickClose: () => void;
  onClickPauseReconciliation: () => void;
  isReconciliationPaused: boolean;
}) {
  const t = useTranslations();
  return (
    <Modal
      title={
        isReconciliationPaused
          ? t("reconciliation.resume_cluster_reconciliation")
          : t("reconciliation.pause_reconciliation")
      }
      isOpen={isModalOpen}
      variant={ModalVariant.medium}
      description={
        isReconciliationPaused ? (
          <></>
        ) : (
          t("reconciliation.pause_reconciliation_description")
        )
      }
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
      {isReconciliationPaused
        ? t("reconciliation.resume_cluster_reconciliation_description")
        : t("reconciliation.pause_reconciliation_text")}
    </Modal>
  );
}
