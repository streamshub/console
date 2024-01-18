"use client";

import { ConsumerGroup } from "@/api/consumerGroups/schema";
import { LabelLink } from "@/components/LabelLink";
import { Number } from "@/components/Number";
import { TableView } from "@/components/table";
import { LabelGroup } from "@/libs/patternfly/react-core";
import { Link } from "@/navigation";
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
    <TableView
      itemCount={consumerGroups?.length}
      page={page}
      onPageChange={() => {}}
      data={consumerGroups}
      emptyStateNoData={<div>No consumer groups</div>}
      emptyStateNoResults={<div>todo</div>}
      ariaLabel={"Consumer groups"}
      columns={["name", "state", "lag", "members", "topics"] as const}
      renderHeader={({ column, key, Th }) => {
        switch (column) {
          case "name":
            return (
              <Th key={key} width={30} dataLabel={"Consumer group name"}>
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
        }
      }}
      renderCell={({ row, column, key, Td }) => {
        switch (column) {
          case "name":
            return (
              <Td key={key} dataLabel={"Consumer group name"}>
                <Link href={`/kafka/${kafkaId}/consumer-groups/${row.id === "" ? "+" : row.id}`}>
                  {row.id === "" ? <i>Empty Name</i> : row.id}
                </Link>
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
                    ?.map((o) => o.lag)
                    // lag values may not be available from API, e.g. when there is an error listing the topic offsets
                    .reduce((acc, v) => (acc ?? NaN) + (v ?? NaN), 0)}
                />
              </Td>
            );
          case "topics":
            const allTopics = row.attributes.members?.flatMap((m) => m.assignments ?? []) ?? [];
            return (
              <Td key={key} dataLabel={"Assigned topics"}>
                <LabelGroup>
                  {Array.from(new Set(allTopics.map((a) => a.topicName))).map(
                    (topic, idx) => (
                      <LabelLink
                        key={idx}
                        color={"blue"}
                        href={`/kafka/${kafkaId}/topics/${
                          allTopics.find((t) => t.topicName === topic)!.topicId
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
              <Td key={key} dataLabel={"Members"}>
                <Number value={row.attributes.members?.length} />
              </Td>
            );
        }
      }}
    />
  );
}
