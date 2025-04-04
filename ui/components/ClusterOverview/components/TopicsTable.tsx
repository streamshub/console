"use client";
import { ViewedTopic } from "@/api/topics/actions";
import { ResponsiveTable } from "@/components/Table";
import { Link } from "@/i18n/routing";
import { Truncate } from "@patternfly/react-core";
import { TableVariant } from "@patternfly/react-table";
import { useTranslations } from "next-intl";
import { ReactNode } from "react";
import { Icon } from "@/libs/patternfly/react-core";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
} from "@/libs/patternfly/react-icons";
import { TopicStatus } from "@/api/topics/schema";

export const TopicsTableColumns = ["name"] as const;
const StatusLabel: Record<TopicStatus, ReactNode> = {
  FullyReplicated: (
    <>
      <Icon status={"success"}>
        <CheckCircleIcon />
      </Icon>
      &nbsp;Fully replicated
    </>
  ),
  UnderReplicated: (
    <>
      <Icon status={"warning"}>
        <ExclamationTriangleIcon />
      </Icon>
      &nbsp;Under replicated
    </>
  ),
  PartiallyOffline: (
    <>
      <Icon status={"warning"}>
        <ExclamationTriangleIcon />
      </Icon>
      &nbsp;Partially offline
    </>
  ),
  Unknown: (
    <>
      <Icon status={"warning"}>
        <ExclamationTriangleIcon />
      </Icon>
      &nbsp;Unknown
    </>
  ),
  Offline: (
    <>
      <Icon status={"danger"}>
        <ExclamationCircleIcon />
      </Icon>
      &nbsp;Offline
    </>
  ),
};

export type TopicsTableProps = {
  topics: ViewedTopic[] | undefined;
};

export function TopicsTable({ topics }: TopicsTableProps) {
  const t = useTranslations("topics");

  return (
    <ResponsiveTable
      data={topics}
      ariaLabel={"Topics"}
      columns={TopicsTableColumns}
      renderHeader={({ Th, column, key }) => {
        switch (column) {
          case "name":
            return (
              <Th key={key} width={70} dataLabel={"Topic"}>
                {t("recently_viewed_topics.topic_name")}
              </Th>
            );
        }
      }}
      renderCell={({ Td, column, row, key }) => {
        switch (column) {
          case "name":
            return (
              <Td key={key} dataLabel={"Topic"}>
                <Link href={`/kafka/${row.kafkaId}/topics/${row.topicId}`}>
                  <Truncate content={row.topicName} />
                </Link>
              </Td>
            );
        }
      }}
      variant={TableVariant.compact}
    />
  );
}
