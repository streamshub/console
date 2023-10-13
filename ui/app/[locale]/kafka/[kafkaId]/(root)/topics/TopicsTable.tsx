"use client";
import { TopicList } from "@/api/topics";
import { Number } from "@/components/Number";
import { TableView } from "@/components/table";
import { TableVariant } from "@patternfly/react-table";
import { useFormatter } from "next-intl";
import Link from "next-intl/link";

export type TopicsTableProps = {
  topics: TopicList[];
  canCreate: boolean;
};

export function TopicsTable({ canCreate, topics }: TopicsTableProps) {
  const format = useFormatter();

  return (
    <TableView
      itemCount={topics.length}
      page={1}
      onPageChange={() => {}}
      data={topics}
      emptyStateNoData={<div>no data</div>}
      emptyStateNoResults={<div>no results</div>}
      ariaLabel={"Topics"}
      columns={["name", "partitions", "messages"] as const}
      renderHeader={({ Th, column, key }) => {
        switch (column) {
          case "name":
            return (
              <Th key={key} width={70}>
                Name
              </Th>
            );
          case "partitions":
            return <Th key={key}>Partitions</Th>;
          case "messages":
            return <Th key={key}>Messages</Th>;
        }
      }}
      renderCell={({ Td, column, row, key }) => {
        switch (column) {
          case "name":
            return (
              <Td key={key}>
                <Link href={`./topics/${row.id}`}>{row.attributes.name}</Link>
              </Td>
            );
          case "partitions":
            return (
              <Td key={key}>
                {format.number(row.attributes.partitions.length)}
              </Td>
            );
          case "messages":
            return (
              <Td key={key}>
                <Number value={row.attributes.recordCount} />
              </Td>
            );
        }
      }}
      filters={{
        name: {
          type: "search",
          chips: [],
          onSearch: () => {},
          onRemoveChip: () => {},
          onRemoveGroup: () => {},
          validate: () => true,
          errorMessage: "",
        },
      }}
      actions={
        canCreate
          ? [
              {
                label: "Add a topic",
                onClick: () => {},
                isPrimary: true,
              },
            ]
          : undefined
      }
      variant={TableVariant.compact}
    />
  );
}
