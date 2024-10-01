"use client";

import {
  Button,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Modal,
  ModalVariant,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";

export function OptimizationProposal({
  numIntraBrokerReplicaMovements,
  numReplicaMovements,
  onDemandBalancednessScoreAfter,
  intraBrokerDataToMoveMB,
  monitoredPartitionsPercentage,
  excludedBrokersForReplicaMove,
  excludedBrokersForLeadership,
  onDemandBalancednessScoreBefore,
  recentWindows,
  dataToMoveMB,
  excludedTopics,
  numLeaderMovements,
  sessionId,
  baseurl,
}: {
  numIntraBrokerReplicaMovements: number;
  numReplicaMovements: number;
  onDemandBalancednessScoreAfter: number;
  intraBrokerDataToMoveMB: number;
  monitoredPartitionsPercentage: number;
  excludedBrokersForReplicaMove: string[] | null | undefined;
  excludedBrokersForLeadership: string[] | null | undefined;
  onDemandBalancednessScoreBefore: number;
  recentWindows: number;
  dataToMoveMB: number;
  excludedTopics: string[] | null | undefined;
  numLeaderMovements: number;
    isModalOpen: boolean;
    sessionId: string | null | undefined;
  baseurl: string;
}) {
  const t = useTranslations("Rebalancing");
  const router = useRouter();

  const onClickClose = () => {
    router.push(`${baseurl}`);
  };
  return (
    <Modal
      title={t("optimization_proposal_of_kafka_rebalance")}
      variant={ModalVariant.medium}
      isOpen={true}
      onClose={onClickClose}
      actions={[
        <Button key="done" variant="primary" onClick={onClickClose}>
          {t("done")}
        </Button>,
        <Button key="cancel" variant="link" onClick={onClickClose}>
          {t("cancel")}
        </Button>,
      ]}
    >
      <DescriptionList
        isHorizontal
        horizontalTermWidthModifier={{
          default: "12ch",
          sm: "15ch",
          md: "20ch",
          lg: "28ch",
          xl: "30ch",
          "2xl": "35ch",
        }}
      >
        <DescriptionListGroup>
          <DescriptionListTerm>{t("data_to_move")}</DescriptionListTerm>
          <DescriptionListDescription>
            {dataToMoveMB}
            {" MB"}
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>
            {t("excluded_brokers_for_leadership")}
          </DescriptionListTerm>
          <DescriptionListDescription>
            {excludedBrokersForLeadership}
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>
            {t("excluded_brokers_for_replica_move")}
          </DescriptionListTerm>
          <DescriptionListDescription>
            {excludedBrokersForReplicaMove}
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>{t("excluded_topics")}</DescriptionListTerm>
          <DescriptionListDescription>
            {excludedTopics}
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>
            {t("intra_broker_data_to_move")}
          </DescriptionListTerm>
          <DescriptionListDescription>
            {intraBrokerDataToMoveMB}
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>
            {t("monitored_partitions_percentage")}
          </DescriptionListTerm>
          <DescriptionListDescription>
            {monitoredPartitionsPercentage}
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>
            {t("num_intra_broker_replica_movements")}
          </DescriptionListTerm>
          <DescriptionListDescription>
            {numIntraBrokerReplicaMovements}
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>{t("num_leader_movements")}</DescriptionListTerm>
          <DescriptionListDescription>
            {numLeaderMovements}
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>
            {t("num_replica_movements")}
          </DescriptionListTerm>
          <DescriptionListDescription>
            {numReplicaMovements}
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>
            {t("on_demand_balancedness_score_after")}
          </DescriptionListTerm>
          <DescriptionListDescription>
            {onDemandBalancednessScoreAfter}
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>
            {t("on_demand_balancedness_Score_before")}
          </DescriptionListTerm>
          <DescriptionListDescription>
            {onDemandBalancednessScoreBefore}
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>{t("recent_windows")}</DescriptionListTerm>
          <DescriptionListDescription>
            {recentWindows}
          </DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>{t("session_id")}</DescriptionListTerm>
          <DescriptionListDescription>{sessionId}</DescriptionListDescription>
        </DescriptionListGroup>
      </DescriptionList>
    </Modal>
  );
}
