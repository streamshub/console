"use client";

import { updateKafkaCluster } from "@/api/kafka/actions";
import { useOpenClusterConnectionPanel } from "@/components/ClusterDrawerContext";
import { ReconciliationModal } from "@/components/ClusterOverview/ReconciliationModal";
import { Button, Flex, FlexItem } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { useState } from "react";

export function ConnectButton({ clusterId }: { clusterId: string }) {
  const t = useTranslations();
  const open = useOpenClusterConnectionPanel();

  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);

  const [isReconciliationPaused, setIsReconciliationPaused] =
    useState<boolean>(false);

  const updateReconciliation = async (reconciliationPaused: boolean) => {
    const success = await updateKafkaCluster(clusterId, reconciliationPaused);

    console.log("hello", success);
    if (success) {
      setIsModalOpen(false);
      setIsReconciliationPaused(true);
    } else {
      alert("update failed");
      setIsModalOpen(false);
    }
  };

  return (
    <>
      <Flex>
        <FlexItem>
          <Button
            variant="secondary"
            onClick={
              isReconciliationPaused
                ? () => setIsModalOpen(true)
                : () => updateReconciliation(false)
            }
          >
            {isReconciliationPaused
              ? t("reconciliation.reconciliation_paused")
              : t("reconciliation.pause_reconciliation_button")}
          </Button>
        </FlexItem>
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
          onClickPauseReconciliation={() => updateReconciliation(true)}
        />
      )}
    </>
  );
}
