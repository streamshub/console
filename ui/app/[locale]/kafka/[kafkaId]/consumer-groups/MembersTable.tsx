import { ConsumerGroup } from "@/api/consumerGroups";
import { AssigmentsTable } from "@/app/[locale]/kafka/[kafkaId]/consumer-groups/AssigmentsTable";
import { Number } from "@/components/Number";
import { ResponsiveTable } from "@/components/table";
import { TableVariant } from "@patternfly/react-table";

export function MembersTable({
  kafkaId,
  members,
}: {
  kafkaId: string;
  members: ConsumerGroup["attributes"]["members"];
}) {
  return (
    <ResponsiveTable
      ariaLabel={"Consumer group members"}
      columns={["member", "client", "host", "assigments"] as const}
      data={members}
      variant={TableVariant.compact}
      renderHeader={({ column, key, Th }) => {
        switch (column) {
          case "member":
            return <Th key={key}>Member</Th>;
          case "client":
            return <Th key={key}>Client</Th>;
          case "host":
            return <Th key={key}>Host</Th>;
          case "assigments":
            return <Th key={key}>Assigments</Th>;
        }
      }}
      renderCell={({ column, key, row, Td }) => {
        switch (column) {
          case "member":
            return (
              <Td key={key} dataLabel={"Member"}>
                {row.memberId}
              </Td>
            );
          case "client":
            return (
              <Td key={key} dataLabel={"Client"}>
                {row.clientId}
              </Td>
            );
          case "host":
            return (
              <Td key={key} dataLabel={"Host"}>
                {row.host}
              </Td>
            );
          case "assigments":
            return (
              <Td key={key} dataLabel={"Assigment"}>
                <Number value={row.assignments.length} />
              </Td>
            );
        }
      }}
      isColumnExpandable={({ column, row }) => {
        switch (column) {
          case "assigments":
            return row.assignments.length > 0;
          default:
            return false;
        }
      }}
      getExpandedRow={({ column, row }) => {
        switch (column) {
          case "assigments":
            return (
              <div className={"pf-v5-u-p-lg"}>
                <AssigmentsTable
                  kafkaId={kafkaId}
                  assigments={row.assignments}
                />
              </div>
            );
          default:
            return false;
        }
      }}
    />
  );
}
