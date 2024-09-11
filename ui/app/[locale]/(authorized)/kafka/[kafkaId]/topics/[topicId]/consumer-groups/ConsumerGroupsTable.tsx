"use client";

import { ConsumerGroup } from "@/api/consumerGroups/schema";
import { Number } from "@/components/Format/Number";
import { LabelLink } from "@/components/Navigation/LabelLink";
import { TableView } from "@/components/Table";
import { LabelGroup, Tooltip } from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { Link } from "@/navigation";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";

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
  consumerGroups: ConsumerGroup[] | undefined;
  refresh: (() => Promise<ConsumerGroup[]>) | undefined;
}) {
  const t = useTranslations();
  const [consumerGroups, setConsumerGroups] = useState(initialData);
  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (refresh) {
      interval = setInterval(async () => {
        const consumerGroups = await refresh();
        setConsumerGroups(consumerGroups);
      }, 5000);
    }
    return () => clearInterval(interval);
  }, [refresh]);
  return (
    <TableView
      itemCount={consumerGroups?.length}
      page={page}
      onPageChange={() => { }}
      data={consumerGroups}
      emptyStateNoData={
        <div>{t("ConsumerGroupsTable.no_consumer_groups")}</div>
      }
      emptyStateNoResults={
        <div>{t("ConsumerGroupsTable.no_consumer_groups")}</div>
      }
      ariaLabel={t("ConsumerGroupsTable.title")}
      columns={["name", "state", "lag", "members", "topics"] as const}
      renderHeader={({ column, key, Th }) => {
        switch (column) {
          case "name":
            return (
              <Th key={key} width={30}>
                {t("ConsumerGroupsTable.consumer_group_name")}
              </Th>
            );
          case "state":
            return (
              <Th key={key}>
                {t("ConsumerGroupsTable.state")}{" "}
                <Tooltip content={t.rich("ConsumerGroupsTable.state_tooltip")}>
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
                  content={t.rich("ConsumerGroupsTable.overall_lag_tooltip")}
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
                  content={t.rich("ConsumerGroupsTable.members_tooltip")}
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
          case "name":
            return (
              <Td
                key={key}
                dataLabel={t("ConsumerGroupsTable.consumer_group_name")}
              >
                <Link
                  href={`/kafka/${kafkaId}/consumer-groups/${row.id === "" ? "+" : encodeURIComponent(row.id)}`}
                >
                  {row.id === "" ? (
                    <i>{t("ConsumerGroupsTable.empty_name")}</i>
                  ) : (
                    row.id
                  )}
                </Link>
              </Td>
            );
          case "state":
            return (
              <Td key={key} dataLabel={t("ConsumerGroupsTable.state")}>
                {row.attributes.state}
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
            const allTopics =
              row.attributes.members?.flatMap((m) => m.assignments ?? []) ?? [];
            return (
              <Td key={key} dataLabel={t("ConsumerGroupsTable.topics")}>
                <LabelGroup>
                  {Array.from(new Set(allTopics.map((a) => a.topicName))).map(
                    (topic, idx) => (
                      <LabelLink
                        key={idx}
                        color={"blue"}
                        href={`/kafka/${kafkaId}/topics/${allTopics.find((t) => t.topicName === topic)!.topicId
                          }`}
                      >
                        {topic}
                      </LabelLink>
                    ),
                  )}
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
