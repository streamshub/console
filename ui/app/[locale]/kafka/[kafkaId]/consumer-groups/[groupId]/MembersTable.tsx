"use client";
import { ConsumerGroup } from "@/api/consumerGroups/schema";
import { LagTable } from "@/app/[locale]/kafka/[kafkaId]/consumer-groups/[groupId]/LagTable";
import { Number } from "@/components/Number";
import { Tooltip } from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { TableVariant } from "@/libs/patternfly/react-table";
import { useEffect, useState } from "react";
import { ResponsiveTable } from "../../../../../../components/Table";

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
    if (consumerGroup.attributes.members?.length === 0) {
      members = [
        {
          memberId: "unknown",
          host: "N/A",
          clientId: "unknown",
          assignments: consumerGroup.attributes.offsets?.map((o) => ({
            topicId: o.topicId,
            topicName: o.topicName,
            partition: o.partition,
          })),
        },
      ];
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
                Client ID{" "}
                <Tooltip
                  content={
                    "The unique identifier assigned to the client (consumer) within the consumer group. A client ID helps identify and manage individual consumers."
                  }
                >
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "overallLag":
            return (
              <Th key={key}>
                Overall lag{" "}
                <Tooltip
                  style={{ whiteSpace: "pre-line" }}
                  content={`The cumulative lag across all partitions assigned to the consumer group.
                       Consumer lag is the difference in the rate of production and consumption of messages.
                       Specifically, consumer lag for a given consumer in a group indicates the delay between the last message in the partition and the message being currently picked up by that consumer.`}
                >
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
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
            const topics = row.assignments?.map((a) => a.topicId);
            return (
              <Td key={key} dataLabel={"Overall lag"}>
                <Number
                  value={consumerGroup!.attributes.offsets
                    ?.filter((o) => topics?.includes(o.topicId))
                    .map((o) => o.lag)
                    // lag values may not be available from API, e.g. when there is an error listing the topic offsets
                    .reduce((acc, v) => (acc ?? NaN) + (v ?? NaN), 0)}
                />
              </Td>
            );
          case "assignedPartitions":
            return (
              <Td key={key} dataLabel={"Assigned partitions"}>
                <Number value={row.assignments?.length} />
              </Td>
            );
        }
      }}
      isRowExpandable={() => {
        return true;
      }}
      getExpandedRow={({ row }) => {
        const offsets: ConsumerGroup["attributes"]["offsets"] =
          row.assignments?.map((a) => ({
            ...a,
            ...consumerGroup!.attributes.offsets?.find(
              (o) => o.topicId === a.topicId && o.partition === a.partition,
            )!,
          }));
        offsets?.sort((a, b) => a.topicName.localeCompare(b.topicName));
        return (
          <div className={"pf-v5-u-p-lg"}>
            <LagTable kafkaId={kafkaId} offsets={offsets} />
          </div>
        );
      }}
    />
  );
}
