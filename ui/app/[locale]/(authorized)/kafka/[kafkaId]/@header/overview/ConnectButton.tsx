"use client";

import { updateKafkaCluster } from "@/api/kafka/actions";
import { useOpenClusterConnectionPanel } from "@/components/ClusterDrawerContext";
import { ReconciliationModal } from "@/components/ClusterOverview/ReconciliationModal";
import { useReconciliationContext } from "@/components/ReconciliationContext";
import { Button, Flex, FlexItem } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { useState } from "react";

export function ConnectButton({
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
      const success = await updateKafkaCluster(clusterId, pausedState);

      if (success) {
        setReconciliationPaused(pausedState);
        setIsModalOpen(false);
      }
    } catch (e: unknown) {
      console.log("Unknown error occurred");
    }
  };

  return (
    <>
      <Flex>
        {managed && (
          <FlexItem>
            <Button
              variant="secondary"
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
          </FlexItem>
        )}
        <FlexItem>
          <Button onClick={() => open(clusterId)}>
            {t("ConnectButton.cluster_connection_details")}
          </Button>
        </FlexItem>
      </Flex>
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
