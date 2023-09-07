"use client";
import { Topic } from "@/api/types";
import { TableView } from "@/components/table";
import { useFormatter } from "next-intl";
import Link from "next/link";

export type TopicsProps = {
  topics: Topic[];
  canCreate: boolean;
};

export function Topics({ canCreate, topics }: TopicsProps) {
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
      columns={
        ["name", "partitions", "retention.ms", "retention.byte"] as const
      }
      renderHeader={({ Th, column, key }) => {
        switch (column) {
          case "name":
            return <Th key={key}>Name</Th>;
          case "partitions":
            return <Th key={key}>Partitions</Th>;
          case "retention.byte":
            return <Th key={key}>Retention byte (bytes)</Th>;
          case "retention.ms":
            return <Th key={key}>Retention time (ms)</Th>;
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
          case "retention.byte":
            return (
              <Td key={key}>
                {row.attributes.configs["retention.bytes"].value}
              </Td>
            );
          case "retention.ms":
            return (
              <Td key={key}>
                {format.number(
                  parseInt(row.attributes.configs["retention.ms"].value, 10),
                )}
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
    />
  );
}
