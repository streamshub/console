"use client";
import { ViewedTopic } from "@/api/topics/actions";
import { ResponsiveTable } from "@/components/Table";
import { Link } from "@/i18n/routing";
import { Truncate } from "@patternfly/react-core";
import { TableVariant, Th } from "@patternfly/react-table";
import { useTranslations } from "next-intl";

export const TopicsTableColumns = ["name"] as const;

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
      renderHeader={({ column, key }) => {
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
