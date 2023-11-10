import { ConsumerGroup } from "@/api/consumerGroups/schema";
import { Number } from "@/components/Number";
import { ResponsiveTable } from "@/components/table";
import { Link } from "@/navigation";
import { TableVariant } from "@patternfly/react-table";

export function AssigmentsTable({
  kafkaId,
  assigments,
}: {
  kafkaId: string;
  assigments: ConsumerGroup["attributes"]["members"][0]["assignments"];
}) {
  return (
    <ResponsiveTable
      ariaLabel={"Consumer group members"}
      columns={["topic", "partition"] as const}
      data={assigments}
      variant={TableVariant.compact}
      renderHeader={({ column, key, Th }) => {
        switch (column) {
          case "topic":
            return <Th key={key}>Topic</Th>;
          case "partition":
            return <Th key={key}>Partition</Th>;
        }
      }}
      renderCell={({ column, key, row, Td }) => {
        switch (column) {
          case "topic":
            return (
              <Td key={key} dataLabel={"Topic"}>
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
        }
      }}
    />
  );
}
