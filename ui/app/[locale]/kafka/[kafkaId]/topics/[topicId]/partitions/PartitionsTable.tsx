"use client";
import { getTopic } from "@/api/topics/actions";
import { Topic } from "@/api/topics/schema";
import { Bytes } from "@/components/Bytes";
import { Number } from "@/components/Number";
import { TableView } from "@/components/table";
import { Label, LabelGroup, PageSection } from "@/libs/patternfly/react-core";
import { WarningTriangleIcon } from "@/libs/patternfly/react-icons";
import { CodeBranchIcon, ServerIcon } from "@patternfly/react-icons";
import Link from "next/link";
import { useEffect, useState } from "react";

export function PartitionsTable({
  topic: initialData,
  kafkaId,
}: {
  kafkaId: string;
  topic: Topic | undefined;
}) {
  const [topic, setTopic] = useState(initialData);
  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (initialData) {
      interval = setInterval(async () => {
        const topic = await getTopic(kafkaId, initialData.id);
        setTopic(topic);
      }, 30000);
    }
    return () => clearInterval(interval);
  }, [kafkaId, initialData]);
  return (
    <PageSection isFilled>
      <TableView
        itemCount={topic?.attributes.partitions?.length}
        page={1}
        onPageChange={() => {}}
        data={topic?.attributes.partitions}
        emptyStateNoData={<div>No partitions</div>}
        emptyStateNoResults={<div>todo</div>}
        ariaLabel={"Partitions"}
        columns={
          [
            "id",
            "replications",
            "offsets",
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
                  {leader !== undefined && (
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
                  )}
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
