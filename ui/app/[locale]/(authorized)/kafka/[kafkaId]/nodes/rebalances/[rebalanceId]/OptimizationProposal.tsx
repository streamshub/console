"use client";

import {
  Button,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalVariant,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
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
  numIntraBrokerReplicaMovements: number | undefined;
  numReplicaMovements: number | undefined;
  onDemandBalancednessScoreAfter: number | undefined;
  intraBrokerDataToMoveMB: number | undefined;
  monitoredPartitionsPercentage: number | undefined;
  excludedBrokersForReplicaMove: string[] | null | undefined;
  excludedBrokersForLeadership: string[] | null | undefined;
  onDemandBalancednessScoreBefore: number | undefined;
  recentWindows: number | undefined;
  dataToMoveMB: number | undefined;
  excludedTopics: string[] | null | undefined;
  numLeaderMovements: number | undefined;
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
    <Modal variant={ModalVariant.medium} isOpen={true} onClose={onClickClose}>
      <ModalHeader
        title={t("optimization_proposal_of_kafka_rebalance")}
        description={t("optimization_proposal_of_kafka_rebalance_description")}
      />
      <ModalBody>
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
            <DescriptionListTerm>
              {t("data_to_move")}{" "}
              <Tooltip content={t("data_to_move_tooltip")}>
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {dataToMoveMB || 0}
              {" MB"}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("excluded_brokers_for_leadership")}{" "}
              <Tooltip content={t("excluded_brokers_for_leadership_tooltip")}>
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {excludedBrokersForLeadership &&
              excludedBrokersForLeadership.length > 0
                ? excludedBrokersForLeadership.join(", ")
                : "-"}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("excluded_brokers_for_replica_move")}{" "}
              <Tooltip content={t("excluded_brokers_for_replica_move_tooltip")}>
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {excludedBrokersForReplicaMove &&
              excludedBrokersForReplicaMove.length > 0
                ? excludedBrokersForReplicaMove.join(", ")
                : "-"}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("excluded_topics")}{" "}
              <Tooltip content={t("excluded_topics_tooltip")}>
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {excludedTopics && excludedTopics.length > 0
                ? excludedTopics.join(", ")
                : "-"}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("intra_broker_data_to_move")}{" "}
              <Tooltip content={t("intra_broker_data_to_move_tooltip")}>
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {intraBrokerDataToMoveMB || 0}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("monitored_partitions_percentage")}{" "}
              <Tooltip content={t("monitored_partitions_percentage_tooltip")}>
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {monitoredPartitionsPercentage || 0}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("num_intra_broker_replica_movements")}{" "}
              <Tooltip
                content={t("num_intra_broker_replica_movements_tooltip")}
              >
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {numIntraBrokerReplicaMovements || 0}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("num_leader_movements")}{" "}
              <Tooltip content={t("num_leader_movements_tooltip")}>
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {numLeaderMovements || 0}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("num_replica_movements")}{" "}
              <Tooltip content={t("num_replica_movements_tooltip")}>
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {numReplicaMovements || 0}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("on_demand_balancedness_score_after")}{" "}
              <Tooltip
                content={t("on_demand_balancedness_score_after_tooltip")}
              >
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {onDemandBalancednessScoreAfter || 0}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("on_demand_balancedness_Score_before")}{" "}
              <Tooltip
                content={t("on_demand_balancedness_Score_before_tooltip")}
              >
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {onDemandBalancednessScoreBefore || 0}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("recent_windows")}{" "}
              <Tooltip content={t("recent_windows_tooltip")}>
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {recentWindows || 0}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("session_id")}{" "}
              <Tooltip content={t("session_id_tooltip")}>
                <HelpIcon />
              </Tooltip>
            </DescriptionListTerm>
            <DescriptionListDescription>
              {sessionId ?? "-"}
            </DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </ModalBody>
      <ModalFooter>
        <Button key="close" variant="primary" onClick={onClickClose}>
          {t("close")}
        </Button>
        ,
      </ModalFooter>
    </Modal>
  );
}
