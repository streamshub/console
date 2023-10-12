"use client";
import { getTopic, Topic } from "@/api/topics";
import { Bytes } from "@/components/Bytes";
import { Number } from "@/components/Number";
import { TableView } from "@/components/table";
import { Label, LabelGroup, PageSection } from "@/libs/patternfly/react-core";
import { WarningTriangleIcon } from "@/libs/patternfly/react-icons";
import { useFormatBytes } from "@/utils/format";
import { CodeBranchIcon, ServerIcon } from "@patternfly/react-icons";
import { useFormatter } from "next-intl";
import Link from "next/link";
import { useEffect, useState } from "react";

export function PartitionsTable({
  topic: initialData,
  kafkaId,
}: {
  kafkaId: string;
  topic: Topic;
}) {
  const format = useFormatter();
  const formatBytes = useFormatBytes();
  const [topic, setTopic] = useState(initialData);
  useEffect(() => {
    const interval = setInterval(async () => {
      const topic = await getTopic(kafkaId, initialData.id);
      setTopic(topic);
    }, 30000);
    return () => clearInterval(interval);
  }, [kafkaId, initialData.id]);
  return (
    <PageSection isFilled>
      <TableView
        itemCount={topic.attributes.partitions.length}
        page={1}
        onPageChange={() => {}}
        data={topic.attributes.partitions}
        emptyStateNoData={<div>No partitions</div>}
        emptyStateNoResults={<div>todo</div>}
        ariaLabel={"Partitions"}
        columns={
          [
            "id",
            "replications",
            "offsets",
            "recordCount",
            "leaderLocalStorage",
          ] as const
        }
        renderHeader={({ column, key, Th }) => {
          switch (column) {
            case "id":
              return (
                <Th key={key} dataLabel={"Partition Id"}>
                  Partition Id
                </Th>
              );
            case "replications":
              return (
                <Th key={key} dataLabel={"Replicas"}>
                  Replications
                </Th>
              );
            case "offsets":
              return (
                <Th key={key} dataLabel={"Offsets"}>
                  Offsets (Earliest/Latest)
                </Th>
              );
            case "recordCount":
              return (
                <Th key={key} dataLabel={"Record Count"}>
                  Record Count
                </Th>
              );
            case "leaderLocalStorage":
              return (
                <Th key={key} dataLabel={"Storage"}>
                  Storage
                </Th>
              );
          }
        }}
        renderCell={({ row, column, key, Td }) => {
          switch (column) {
            case "id":
              return (
                <Td key={key} dataLabel={"Partition Id"}>
                  <Label color={"blue"} icon={<CodeBranchIcon />}>
                    {row.partition}
                  </Label>
                </Td>
              );
            case "replications":
              const leader = row.replicas.find(
                (r) => r.nodeId === row.leaderId,
              )!;
              return (
                <Td key={key} dataLabel={"Replicas"}>
                  <LabelGroup categoryName={"Leader"}>
                    <Label
                      color={"orange"}
                      render={({ className, content }) => (
                        <>
                          <Link
                            className={className}
                            href={`../../nodes/${leader.nodeId}`}
                          >
                            {content}
                          </Link>
                        </>
                      )}
                      icon={<ServerIcon />}
                    >
                      {leader.nodeId}
                    </Label>
                  </LabelGroup>
                  <LabelGroup categoryName={"Replicas"}>
                    {row.replicas
                      .filter((r) => r.nodeId !== row.leaderId)
                      .map((r, idx) => (
                        <Label
                          key={idx}
                          color={!r.inSync ? "red" : "orange"}
                          render={({ className, content }) => (
                            <>
                              <Link className={className} href={"../../nodes"}>
                                {content}
                              </Link>
                            </>
                          )}
                          icon={
                            !r.inSync ? <WarningTriangleIcon /> : <ServerIcon />
                          }
                        >
                          {r.nodeId}
                        </Label>
                      ))}
                  </LabelGroup>
                </Td>
              );
            case "offsets":
              return (
                <Td key={key} dataLabel={"Offset"}>
                  <Number value={row.offsets?.earliest?.offset} />/
                  <Number value={row.offsets?.latest?.offset} />
                </Td>
              );
            case "recordCount":
              return (
                <Td key={key} dataLabel={"Record Count"}>
                  <Number value={row.recordCount} />
                </Td>
              );
            case "leaderLocalStorage":
              return (
                <Td key={key} dataLabel={"Storage"}>
                  <Bytes value={row.leaderLocalStorage} />
                </Td>
              );
          }
        }}
      />
    </PageSection>
  );
}
