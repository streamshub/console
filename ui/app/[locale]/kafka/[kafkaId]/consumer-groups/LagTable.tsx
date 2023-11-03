import { ConsumerGroup } from "@/api/consumerGroups";
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
            return <Th key={key}>Lagging topic</Th>;
          case "partition":
            return <Th key={key}>Partition</Th>;
          case "behind":
            return <Th key={key}>Messages behind</Th>;
          case "currentOffset":
            return <Th key={key}>Current offset</Th>;
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
                  {row.topicId}
                </Link>
              </Td>
            );
          case "partition":
            return (
              <Td key={key} dataLabel={"Partition"}>
                {row.partition}
              </Td>
            );
          case "behind":
            return (
              <Td key={key} dataLabel={"Messages behind"}>
                {row.lag}
              </Td>
            );
          case "currentOffset":
            return (
              <Td key={key} dataLabel={"Current offset"}>
                {row.offset}
              </Td>
            );
          case "endOffset":
            return (
              <Td key={key} dataLabel={"End offset"}>
                {row.offset + row.lag}
              </Td>
            );
        }
      }}
    />
  );
}
