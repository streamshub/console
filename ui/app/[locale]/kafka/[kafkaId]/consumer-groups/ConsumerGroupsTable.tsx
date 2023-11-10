"use client";

import { ConsumerGroup } from "@/api/consumerGroups/schema";
import { LagTable } from "@/app/[locale]/kafka/[kafkaId]/consumer-groups/LagTable";
import { MembersTable } from "@/app/[locale]/kafka/[kafkaId]/consumer-groups/MembersTable";
import { Number } from "@/components/Number";
import { TableView } from "@/components/table";
import { PageSection } from "@/libs/patternfly/react-core";
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
    <PageSection isFilled>
      <TableView
        itemCount={consumerGroups?.length}
        page={page}
        onPageChange={() => {}}
        data={consumerGroups}
        emptyStateNoData={<div>No consumer groups</div>}
        emptyStateNoResults={<div>todo</div>}
        ariaLabel={"Consumer groups"}
        columns={
          ["name", "state", "lag", "members", "topics", "partitions"] as const
        }
        renderHeader={({ column, key, Th }) => {
          switch (column) {
            case "name":
              return (
                <Th key={key} dataLabel={"Consumer group name"}>
                  Consumer group name
                </Th>
              );
            case "state":
              return (
                <Th key={key} dataLabel={"State"}>
                  State
                </Th>
              );
            case "lag":
              return (
                <Th key={key} dataLabel={"Overall lag"}>
                  Overall lag
                </Th>
              );
            case "members":
              return (
                <Th key={key} dataLabel={"Members"}>
                  Members
                </Th>
              );
            case "topics":
              return (
                <Th key={key} dataLabel={"Topics"}>
                  Topics
                </Th>
              );
            case "partitions":
              return (
                <Th key={key} dataLabel={"Assigned partitions"}>
                  Assigned partitions
                </Th>
              );
          }
        }}
        renderCell={({ row, column, key, Td }) => {
          switch (column) {
            case "name":
              return (
                <Td key={key} dataLabel={"Consumer group name"}>
                  {row.id}
                </Td>
              );
            case "state":
              return (
                <Td key={key} dataLabel={"State"}>
                  {row.attributes.state}
                </Td>
              );
            case "lag":
              return (
                <Td key={key} dataLabel={"Overall lag"}>
                  <Number
                    value={row.attributes.offsets
                      .map((o) => o.lag)
                      .reduce((acc, v) => acc + v, 0)}
                  />
                </Td>
              );
            case "topics":
              return (
                <Td key={key} dataLabel={"Assigned topics"}>
                  <Number
                    value={
                      new Set(
                        row.attributes.members.flatMap((m) =>
                          m.assignments.map((a) => a.topicId),
                        ),
                      ).size
                    }
                  />
                </Td>
              );
            case "partitions":
              return (
                <Td key={key} dataLabel={"Assigned partitions"}>
                  <Number
                    value={
                      new Set(
                        row.attributes.members.flatMap((m) =>
                          m.assignments.map((a) => a.partition),
                        ),
                      ).size
                    }
                  />
                </Td>
              );
            case "members":
              return (
                <Td key={key} dataLabel={"Members"}>
                  <Number value={row.attributes.members.length} />
                </Td>
              );
          }
        }}
        isColumnExpandable={({ column, row }) => {
          switch (column) {
            case "lag":
              return row.attributes.offsets.length > 0;
            case "members":
              return row.attributes.members.length > 0;
            case "topics":
              return true;
            default:
              return false;
          }
        }}
        getExpandedRow={({ column, row }) => {
          switch (column) {
            case "lag":
              return (
                <div className={"pf-v5-u-p-lg"}>
                  <LagTable
                    kafkaId={kafkaId}
                    offsets={row.attributes.offsets}
                  />
                </div>
              );
            case "members":
              return (
                <div className={"pf-v5-u-p-lg"}>
                  <MembersTable
                    kafkaId={kafkaId}
                    members={row.attributes.members}
                  />
                </div>
              );
            case "topics":
              return true;
            default:
              return null;
          }
        }}
      />
    </PageSection>
  );
}
