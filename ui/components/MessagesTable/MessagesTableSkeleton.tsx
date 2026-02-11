import { ResponsiveTable } from "@/components/Table";
import { PageSection } from "@/libs/patternfly/react-core";
import { TableVariant, Th } from "@/libs/patternfly/react-table";
import { useTranslations } from "next-intl";
import { columns } from "./components/ColumnsModal";
import { MessagesTableToolbar } from "./components/MessagesTableToolbar";
import { MessagesTableProps } from "./MessagesTable";

export function MessagesTableSkeleton({
  filterLimit,
  filterQuery,
  filterWhere,
  filterPartition,
  filterTimestamp,
  filterOffset,
  filterEpoch,
}: Pick<
  MessagesTableProps,
  | "filterPartition"
  | "filterLimit"
  | "filterQuery"
  | "filterWhere"
  | "filterTimestamp"
  | "filterOffset"
  | "filterEpoch"
>) {
  const t = useTranslations("message-browser");
  return (
    <PageSection
      isFilled={true}
      hasOverflowScroll={true}
      style={{ height: "calc(100vh - 170px - 70px)" }}
      aria-label={t("title")}
    >
      <MessagesTableToolbar
        topicName={""}
        messages={[]}
        partitions={1}
        filterLimit={filterLimit}
        filterQuery={filterQuery}
        filterWhere={filterWhere}
        filterOffset={filterOffset}
        filterEpoch={filterEpoch}
        filterTimestamp={filterTimestamp}
        filterPartition={filterPartition}
        onSearch={() => {}}
        onColumnManagement={() => {}}
      />
      <ResponsiveTable
        variant={TableVariant.compact}
        ariaLabel={t("table_aria_label")}
        columns={columns}
        data={undefined}
        expectedLength={typeof filterLimit === "number" ? filterLimit : 50}
        renderCell={({ Td, key }) => <Td key={key}></Td>}
        renderHeader={({ key }) => <Th key={key}></Th>}
      />
    </PageSection>
  );
}
