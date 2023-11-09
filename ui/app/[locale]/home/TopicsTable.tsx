"use client";
import { TopicList } from "@/api/topics";
import { ButtonLink } from "@/components/ButtonLink";
import { ResponsiveTable } from "@/components/table";
import { TableVariant } from "@patternfly/react-table";
import { useTranslations } from "next-intl";

export const TopicsTableColumns = ["name", "cluster"] as const;

export type TopicsTableProps = {
  topics: TopicList[] | undefined;
};

export function TopicsTable({ topics }: TopicsTableProps) {
  const t = useTranslations("topics");

  return (
    <ResponsiveTable
      data={topics}
      ariaLabel={"Topics"}
      columns={TopicsTableColumns}
      disableAutomaticColumns={true}
      renderHeader={({ Th, column, key }) => {
        switch (column) {
          case "name":
            return (
              <Th key={key} dataLabel={"Topic"}>
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
                <ButtonLink variant={"link"} href={`/kafka/TODO/${row.id}`}>
                  {row.attributes.name}
                </ButtonLink>
              </Td>
            );
          case "cluster":
            return (
              <Td key={key} dataLabel={"Cluster"}>
                <ButtonLink variant={"link"} href={`/kafka/TODO`}>
                  TODO
                </ButtonLink>
              </Td>
            );
        }
      }}
      variant={TableVariant.compact}
    />
  );
}
