import { ConsumerGroup } from "@/api/consumerGroups/schema";
import { Number } from "@/components/Number";
import { ResponsiveTable } from "@/components/Table";
import { Tooltip } from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { Link } from "@/navigation";
import { TableVariant } from "@patternfly/react-table";

export function LagTable({
  kafkaId,
  offsets,
}: {
  kafkaId: string;
  offsets: ConsumerGroup["attributes"]["offsets"];
}) {
  return (
    <ResponsiveTable
      ariaLabel={"Consumer group lag"}
      columns={
        ["topic", "partition", "behind", "currentOffset", "endOffset"] as const
      }
      data={offsets}
      variant={TableVariant.compact}
      renderHeader={({ column, key, Th }) => {
        switch (column) {
          case "topic":
            return (
              <Th key={key} width={30}>
                Topic
              </Th>
            );
          case "partition":
            return <Th key={key}>Partition</Th>;
          case "behind":
            return <Th key={key}>Lag</Th>;
          case "currentOffset":
            return (
              <Th key={key}>
                Committed offset{" "}
                <Tooltip
                  content={
                    "The offset in the Kafka topic marking the last successfully consumed message by the consumer."
                  }
                >
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "endOffset":
            return (
              <Th key={key}>
                End offset{" "}
                <Tooltip
                  content={
                    "The highest offset in the Kafka topic, representing the latest available message."
                  }
                >
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
        }
      }}
      renderCell={({ column, key, row, Td }) => {
        switch (column) {
          case "topic":
            return (
              <Td key={key} dataLabel={"Lagging topic"}>
                <Link href={`/kafka/${kafkaId}/topics/${row.topicId}`}>
                  {row.topicName}
                </Link>
              </Td>
            );
          case "partition":
            return (
              <Td key={key} dataLabel={"Partition"}>
                <Number value={row.partition} />
              </Td>
            );
          case "behind":
            return (
              <Td key={key} dataLabel={"Messages behind"}>
                <Number value={row.lag} />
              </Td>
            );
          case "currentOffset":
            return (
              <Td key={key} dataLabel={"Current offset"}>
                <Number value={row.offset} />
              </Td>
            );
          case "endOffset":
            return (
              <Td key={key} dataLabel={"End offset"}>
                <Number value={row.logEndOffset} />
              </Td>
            );
        }
      }}
    />
  );
}
