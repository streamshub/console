"use client";

import { updateKafkaCluster } from "@/api/kafka/actions";
import { useOpenClusterConnectionPanel } from "@/components/ClusterDrawerContext";
import { ReconciliationModal } from "@/components/ClusterOverview/ReconciliationModal";
import {
  Button,
  Flex,
  FlexItem,
  Grid,
  GridItem,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { useState } from "react";

export function ConnectButton({ clusterId }: { clusterId: string }) {
  const t = useTranslations();
  const open = useOpenClusterConnectionPanel();

  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);

  const handleSave = async () => {
    try {
      const success = await updateKafkaCluster(clusterId, true);
      if (success) {
        setIsModalOpen(false);
      }
    } catch (e: unknown) {
      console.log("Unknown error occurred");
    }
  };

  return (
    <>
      <Flex>
        <FlexItem>
          <Button variant="secondary" onClick={() => setIsModalOpen(true)}>
            {t("reconciliation.pause_reconciliation_button")}
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
          onClickPauseReconciliation={handleSave}
        />
      )}
    </>
  );
}
