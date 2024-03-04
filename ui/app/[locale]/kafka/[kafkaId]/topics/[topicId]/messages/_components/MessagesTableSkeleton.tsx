import { columns } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/ColumnsModal";
import { MessageBrowserProps } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/MessagesTable";
import { MessagesTableToolbar } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/MessagesTableToolbar";
import { ResponsiveTable } from "@/components/Table";
import { PageSection } from "@/libs/patternfly/react-core";
import { TableVariant } from "@/libs/patternfly/react-table";
import { useTranslations } from "next-intl";

export function MessagesTableSkeleton({
  filterLimit,
  filterLive,
  filterQuery,
  filterWhere,
  filterPartition,
  filterTimestamp,
  filterOffset,
  filterEpoch,
}: Pick<
  MessageBrowserProps,
  | "filterPartition"
  | "filterLimit"
  | "filterLive"
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
        partitions={1}
        filterLimit={filterLimit}
        filterLive={filterLive}
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
        expectedLength={filterLimit}
        renderCell={() => <div></div>}
        renderHeader={() => <div></div>}
      />
    </PageSection>
  );
}
