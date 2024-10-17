
"use client";

import { Banner, Bullseye, Button, FlexItem, Flex } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { useReconciliationContext } from "./ReconciliationContext";
import { updateKafkaCluster } from "@/api/kafka/actions";
import { ClusterDetail } from "@/api/kafka/schema";

export function ReconciliationPausedBanner({ kafkaCluster }: { kafkaCluster: ClusterDetail; }) {
  const t = useTranslations();

  const { isReconciliationPaused, setReconciliationPaused } = useReconciliationContext();

  setReconciliationPaused(kafkaCluster.meta?.reconciliationPaused ?? false);

  const resumeReconciliation = async () => {
    if (!kafkaCluster) {
      console.log("kafkaCluster is undefined");
      return;
    }

    try {
      const success = await updateKafkaCluster(kafkaCluster.id, false);

      if (success) {
        setReconciliationPaused(false);
      }
    } catch (e: unknown) {
      console.log("Unknown error occurred");
    }
  }

  return (
    isReconciliationPaused && (
      <Banner variant="gold">
        <Bullseye>
          <Flex>
            <FlexItem spacer={{ default: "spacerNone" }}>
              {t("reconciliation.reconciliation_paused_warning")}
            </FlexItem>
            &nbsp;
            <FlexItem spacer={{ default: "spacerLg" }}>
              <Button variant="link" isInline onClick={resumeReconciliation}>
                {t("reconciliation.resume")}
              </Button>
            </FlexItem>
          </Flex>
        </Bullseye>
      </Banner>
    )
  )
}

