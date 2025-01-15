"use client";

import { updateKafkaCluster } from "@/api/kafka/actions";
import { useOpenClusterConnectionPanel } from "@/components/ClusterDrawerContext";
import { ReconciliationModal } from "@/components/ClusterOverview/ReconciliationModal";
import { useReconciliationContext } from "@/components/ReconciliationContext";
import { Button, Flex, FlexItem } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { PauseCircleIcon, PlayIcon } from "@/libs/patternfly/react-icons";

export function ReconciliationPauseButton({
  clusterId,
  managed,
}: {
  clusterId: string;
  managed: boolean;
}) {
  const t = useTranslations();
  const open = useOpenClusterConnectionPanel();

  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);

  const { isReconciliationPaused, setReconciliationPaused } =
    useReconciliationContext();

  const onClickUpdate = async (pausedState: boolean) => {
    try {
      const response = await updateKafkaCluster(clusterId, pausedState);

      if (response.errors) {
        console.log("Unknown error occurred", response.errors);
      } else {
        setReconciliationPaused(pausedState);
        setIsModalOpen(false);
      }
    } catch (e: unknown) {
      console.log("Unknown error occurred");
    }
  };

  return (
    <>
      {managed && (
        <Button
          variant="link"
          icon={isReconciliationPaused ? <PlayIcon /> : <PauseCircleIcon />}
          onClick={
            isReconciliationPaused
              ? () => onClickUpdate(false)
              : () => setIsModalOpen(true)
          }
        >
          {isReconciliationPaused
            ? t("reconciliation.resume_reconciliation")
            : t("reconciliation.pause_reconciliation_button")}
        </Button>
      )}
      {isModalOpen && (
        <ReconciliationModal
          isModalOpen={isModalOpen}
          onClickClose={() => setIsModalOpen(false)}
          onClickPauseReconciliation={() => onClickUpdate(true)}
        />
      )}
    </>
  );
}
