"use client";

import { ConsumerGroup, ConsumerGroupState } from "@/api/consumerGroups/schema";
import { Number } from "@/components/Format/Number";
import { LabelLink } from "@/components/Navigation/LabelLink";
import { TableView } from "@/components/Table";
import { Icon, LabelGroup, Tooltip } from "@/libs/patternfly/react-core";
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  HelpIcon,
  HistoryIcon,
  InfoCircleIcon,
  InProgressIcon,
  OffIcon,
  PendingIcon,
  SyncAltIcon,
} from "@/libs/patternfly/react-icons";
import { Link } from "@/i18n/routing";
import { useTranslations } from "next-intl";
import { ReactNode, useEffect, useState } from "react";
import RichText from "@/components/RichText";
import { TableVariant } from "@patternfly/react-table";

const StateLabel: Record<ConsumerGroupState, { label: ReactNode }> = {
  STABLE: {
    label: (
      <>
        <Icon status={"success"}>
          <CheckCircleIcon />
        </Icon>
        &nbsp;Stable
      </>
    ),
  },
  EMPTY: {
    label: (
      <>
        <Icon status={"info"}>
          <InfoCircleIcon />
        </Icon>
        &nbsp;Empty
      </>
    ),
  },
  UNKNOWN: {
    label: (
      <>
        <Icon status={"warning"}>
          <ExclamationTriangleIcon />
        </Icon>
        &nbsp;Unknown
      </>
    ),
  },
  PREPARING_REBALANCE: {
    label: (
      <>
        <Icon>
          <PendingIcon />
        </Icon>
        &nbsp;Preparing Rebalance
      </>
    ),
  },
  ASSIGNING: {
    label: (
      <>
        <Icon>
          <HistoryIcon />
        </Icon>
        &nbsp;Assigning
      </>
    ),
  },
  DEAD: {
    label: (
      <>
        <Icon>
          <OffIcon />
        </Icon>
        &nbsp;Dead
      </>
    ),
  },
  COMPLETING_REBALANCE: {
    label: (
      <>
        <Icon>
          <SyncAltIcon />
        </Icon>
        &nbsp;Completing Rebalance
      </>
    ),
  },
  RECONCILING: {
    label: (
      <>
        <Icon>
          <InProgressIcon />
        </Icon>
        &nbsp;Reconciling
      </>
    ),
  },
};

export function ConsumerGroupsTable({
  kafkaId,
  page,
  total,
  consumerGroups: initialData,
  refresh,
}: {
  kafkaId: string;
  page: number;
  total: number;
  consumerGroups?: ConsumerGroup[];
  refresh?: () => Promise<ConsumerGroup[] | null>;
}) {
  const t = useTranslations();
  const [consumerGroups, setConsumerGroups] = useState(initialData);
  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (refresh) {
      interval = setInterval(async () => {
        const consumerGroups = await refresh();
        if (consumerGroups != null) {
          setConsumerGroups(consumerGroups);
        }
      }, 5000);
    }
    return () => clearInterval(interval);
  }, [refresh]);
  return (
    <TableView
      variant={TableVariant.compact}
      itemCount={consumerGroups?.length}
      page={page}
      onPageChange={() => {}}
      data={consumerGroups}
      emptyStateNoData={
        <div>{t("ConsumerGroupsTable.no_consumer_groups")}</div>
      }
      emptyStateNoResults={
        <div>{t("ConsumerGroupsTable.no_consumer_groups")}</div>
      }
      ariaLabel={t("ConsumerGroupsTable.title")}
      columns={["groupId", "type", "protocol", "state", "lag", "members", "topics"] as const}
      renderHeader={({ column, key, Th }) => {
        switch (column) {
          case "groupId":
            return (
              <Th key={key}>
                {t("ConsumerGroupsTable.group_id")}
              </Th>
            );
          case "type":
            return (
              <Th key={key}>
                Type
              </Th>
            );
          case "protocol":
            return (
              <Th key={key}>
                Protocol
              </Th>
            );
          case "state":
            return (
              <Th key={key}>
                {t("ConsumerGroupsTable.state")}{" "}
                <Tooltip
                  content={
                    <RichText>
                      {(tags) =>
                        t.rich("ConsumerGroupsTable.state_tooltip", tags)
                      }
                    </RichText>
                  }
                >
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "lag":
            return (
              <Th key={key}>
                {t("ConsumerGroupsTable.overall_lag")}{" "}
                <Tooltip
                  style={{ whiteSpace: "pre-line" }}
                  content={
                    <RichText>
                      {(tags) =>
                        t.rich("ConsumerGroupsTable.overall_lag_tooltip", tags)
                      }
                    </RichText>
                  }
                >
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "members":
            return (
              <Th key={key}>
                {t("ConsumerGroupsTable.members")}{" "}
                <Tooltip
                  content={
                    <RichText>
                      {(tags) =>
                        t.rich("ConsumerGroupsTable.members_tooltip", tags)
                      }
                    </RichText>
                  }
                >
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "topics":
            return <Th key={key}>{t("ConsumerGroupsTable.topics")}</Th>;
        }
      }}
      renderCell={({ row, column, key, Td }) => {
        switch (column) {
          case "groupId":
            return (
              <Td
                key={key}
                dataLabel={t("ConsumerGroupsTable.group_id")}
              >
                {row.attributes.protocol === "consumer" && row.meta?.describeAvailable
                    ? <Link href={`/kafka/${kafkaId}/groups/${row.id}`}>
                        {row.attributes.groupId}
                      </Link>
                    : <>{row.attributes.groupId}</>
                }
              </Td>
            );
          case "type":
            return (
              <Td key={key} dataLabel={"Type"}>
                {row.attributes.type}
              </Td>
            );
          case "protocol":
            return (
              <Td key={key} dataLabel={"Protocol"}>
                {row.attributes.protocol ?? "-"}
              </Td>
            );
          case "state":
            return (
              <Td key={key} dataLabel={t("ConsumerGroupsTable.state")}>
                {StateLabel[row.attributes.state]?.label}
              </Td>
            );
          case "lag":
            return (
              <Td key={key} dataLabel={t("ConsumerGroupsTable.overall_lag")}>
                <Number
                  value={row.attributes.offsets
                    ?.map((o) => o.lag)
                    // lag values may not be available from API, e.g. when there is an error listing the topic offsets
                    .reduce((acc, v) => (acc ?? NaN) + (v ?? NaN), 0)}
                />
              </Td>
            );
          case "topics":
            const allTopics: Record<string, string | undefined> = {};
            row.attributes.members
              ?.flatMap((m) => m.assignments ?? [])
              .forEach((a) => (allTopics[a.topicName] = a.topicId));
            row.attributes.offsets?.forEach(
              (a) => (allTopics[a.topicName] = a.topicId),
            );
            return (
              <Td key={key} dataLabel={t("ConsumerGroupsTable.topics")}>
                <LabelGroup>
                  {Object.entries(allTopics).map(([topicName, topicId]) => (
                    <LabelLink
                      key={topicName}
                      color={"blue"}
                      href={
                        topicId ? `/kafka/${kafkaId}/topics/${topicId}` : "#"
                      }
                    >
                      {topicName}
                    </LabelLink>
                  ))}
                </LabelGroup>
              </Td>
            );
          case "members":
            return (
              <Td key={key} dataLabel={t("ConsumerGroupsTable.members")}>
                <Number value={row.attributes.members?.length} />
              </Td>
            );
        }
      }}
    />
  );
}
