import { ConsumerGroup } from "@/api/consumerGroups/schema";
import { Number } from "@/components/Number";
import { ResponsiveTable } from "@/components/table";
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
            return <Th key={key}>Committed offset</Th>;
          case "endOffset":
            return <Th key={key}>End offset</Th>;
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
                <Number value={row.offset + row.lag} />
              </Td>
            );
        }
      }}
    />
  );
}
