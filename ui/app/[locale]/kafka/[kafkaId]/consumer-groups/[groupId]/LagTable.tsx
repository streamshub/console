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
      columns={["topic", "partition", "behind", "offset"] as const}
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
          case "offset":
            return <Th key={key}>Offset</Th>;
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
          case "offset":
            return (
              <Td key={key} dataLabel={"Offset"}>
                <Number value={row.offset + row.lag} />
              </Td>
            );
        }
      }}
    />
  );
}
