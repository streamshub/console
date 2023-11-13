"use client";
import { ViewedTopic } from "@/api/topics/actions";
import { ResponsiveTable } from "@/components/table";
import { Link } from "@/navigation";
import { Truncate } from "@patternfly/react-core";
import { TableVariant } from "@patternfly/react-table";
import { useTranslations } from "next-intl";

export const TopicsTableColumns = ["name", "cluster"] as const;

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
                Name
              </Th>
            );
          case "cluster":
            return (
              <Th key={key} dataLabel={"Cluster"}>
                Cluster
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
          case "cluster":
            return (
              <Td key={key} dataLabel={"Cluster"}>
                <Link href={`/kafka/${row.kafkaId}`}>{row.kafkaName}</Link>
              </Td>
            );
        }
      }}
      variant={TableVariant.compact}
    />
  );
}
