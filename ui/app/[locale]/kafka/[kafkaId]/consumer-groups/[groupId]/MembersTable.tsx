"use client";
import { ConsumerGroup } from "@/api/consumerGroups/schema";
import { LagTable } from "@/app/[locale]/kafka/[kafkaId]/consumer-groups/[groupId]/LagTable";
import { Number } from "@/components/Number";
import { ResponsiveTable } from "@/components/table";
import { TableVariant } from "@/libs/patternfly/react-table";
import { useEffect, useState } from "react";

export function MembersTable({
  kafkaId,
  consumerGroup: initialData,
  refresh,
}: {
  kafkaId: string;
  consumerGroup?: ConsumerGroup;
  refresh?: () => Promise<ConsumerGroup>;
}) {
  const [consumerGroup, setConsumerGroup] = useState(initialData);
  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (refresh) {
      interval = setInterval(async () => {
        const cg = await refresh();
        setConsumerGroup(cg);
      }, 5000);
    }
    return () => clearInterval(interval);
  }, [refresh]);
  let members: ConsumerGroup["attributes"]["members"] | undefined = undefined;

  if (consumerGroup) {
    if (consumerGroup.attributes.members.length === 0) {
      members = consumerGroup.attributes.offsets.map((o) => ({
        memberId: "unknown",
        host: "N/A",
        clientId: "unknown",
        assignments: consumerGroup.attributes.offsets.map((o) => ({
          topicId: o.topicId,
          topicName: o.topicName,
          partition: o.partition,
        })),
      }));
    } else {
      members = consumerGroup.attributes.members;
    }
  }
  return (
    <ResponsiveTable
      ariaLabel={"Consumer group consumerGroup"}
      columns={
        ["member", "clientId", "overallLag", "assignedPartitions"] as const
      }
      data={members}
      variant={TableVariant.compact}
      renderHeader={({ column, key, Th }) => {
        switch (column) {
          case "member":
            return (
              <Th width={30} key={key}>
                Member ID
              </Th>
            );
          case "clientId":
            return (
              <Th width={20} key={key}>
                Client ID
              </Th>
            );
          case "overallLag":
            return <Th key={key}>Overall lag</Th>;
          case "assignedPartitions":
            return <Th key={key}>Assigned partitions</Th>;
        }
      }}
      renderCell={({ column, key, row, Td }) => {
        switch (column) {
          case "member":
            return (
              <Td key={key} dataLabel={"Member ID"}>
                {row.memberId}
              </Td>
            );
          case "clientId":
            return (
              <Td key={key} dataLabel={"Client ID"}>
                {row.clientId}
              </Td>
            );
          case "overallLag":
            const topics = row.assignments.map((a) => a.topicId);
            return (
              <Td key={key} dataLabel={"Overall lag"}>
                <Number
                  value={consumerGroup!.attributes.offsets
                    .filter((o) => topics.includes(o.topicId))
                    .map((o) => o.lag)
                    .reduce((acc, v) => acc + v, 0)}
                />
              </Td>
            );
          case "assignedPartitions":
            return (
              <Td key={key} dataLabel={"Assigned partitions"}>
                <Number value={row.assignments.length} />
              </Td>
            );
        }
      }}
      isRowExpandable={() => {
        return true;
      }}
      getExpandedRow={({ row }) => {
        const offsets: ConsumerGroup["attributes"]["offsets"] =
          row.assignments.map((a) => ({
            ...a,
            ...consumerGroup!.attributes.offsets.find(
              (o) => o.topicId === a.topicId && o.partition === a.partition,
            )!,
          }));
        offsets.sort((a, b) => a.topicName.localeCompare(b.topicName));
        return (
          <div className={"pf-v5-u-p-lg"}>
            <LagTable kafkaId={kafkaId} offsets={offsets} />
          </div>
        );
      }}
    />
  );
}
