
import { ConsumerGroup, GroupType, GroupTypeValues, ConsumerGroupState } from "@/api/consumerGroups/schema";
import { Number } from "@/components/Format/Number";
import { LabelLink } from "@/components/Navigation/LabelLink";
import { TableView, TableViewProps } from "@/components/Table";
import { Icon, LabelGroup, Tooltip } from "@/libs/patternfly/react-core";
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  HelpIcon,
  HistoryIcon,
  InfoCircleIcon,
  InProgressIcon,
  OffIcon,
  PendingIcon,
  SyncAltIcon,
} from "@/libs/patternfly/react-icons";
import { Link } from "@/i18n/routing";
import { useTranslations } from "next-intl";
import { ReactNode } from "react";
import { EmptyStateNoMatchFound } from "@/components/Table/EmptyStateNoMatchFound";
import RichText from "@/components/RichText";
import { hasPrivilege } from "@/utils/privileges";
import { TableVariant } from "@patternfly/react-table";

export const ConsumerGroupColumns = [
  "groupId",
  "type",
  "protocol",
  "state",
  "lag",
  "members",
  "topics",
] as const;

export type ConsumerGroupColumn = (typeof ConsumerGroupColumns)[number];

export type SortableConsumerGroupTableColumns = Exclude<
  ConsumerGroupColumn,
  "lag" | "members" | "topics"
>;

export const SortableColumns = ["groupId", "type", "protocol", "state"];

const StateLabel: Record<ConsumerGroupState, { label: ReactNode }> = {
  STABLE: {
    label: (
      <>
        <Icon status={"success"}>
          <CheckCircleIcon />
        </Icon>
        &nbsp;Stable
      </>
    ),
  },
  EMPTY: {
    label: (
      <>
        <Icon status={"info"}>
          <InfoCircleIcon />
        </Icon>
        &nbsp;Empty
      </>
    ),
  },
  UNKNOWN: {
    label: (
      <>
        <Icon status={"warning"}>
          <ExclamationTriangleIcon />
        </Icon>
        &nbsp;Unknown
      </>
    ),
  },
  PREPARING_REBALANCE: {
    label: (
      <>
        <Icon>
          <PendingIcon />
        </Icon>
        &nbsp;Preparing Rebalance
      </>
    ),
  },
  ASSIGNING: {
    label: (
      <>
        <Icon>
          <HistoryIcon />
        </Icon>
        &nbsp;Assigning
      </>
    ),
  },
  DEAD: {
    label: (
      <>
        <Icon>
          <OffIcon />
        </Icon>
        &nbsp;Dead
      </>
    ),
  },
  COMPLETING_REBALANCE: {
    label: (
      <>
        <Icon>
          <SyncAltIcon />
        </Icon>
        &nbsp;Completing Rebalance
      </>
    ),
  },
  RECONCILING: {
    label: (
      <>
        <Icon>
          <InProgressIcon />
        </Icon>
        &nbsp;Reconciling
      </>
    ),
  },
};

export function ConsumerGroupsTable({
  kafkaId,
  page,
  perPage,
  total,
  consumerGroups,
  isColumnSortable,
  filterName,
  filterType,
  filterState,
  onFilterNameChange,
  onFilterTypeChange,
  onFilterStateChange,
  onPageChange,
  onResetOffset,
  onClearAllFilters,
}: {
  kafkaId: string;
  page: number;
  perPage: number;
  total: number;
  filterName: string | undefined;
  filterType: GroupType[] | undefined;
  filterState: ConsumerGroupState[] | undefined;
  consumerGroups: ConsumerGroup[] | undefined;
  onFilterNameChange: (name: string | undefined) => void;
  onFilterTypeChange: (type: GroupType[] | undefined) => void;
  onFilterStateChange: (state: ConsumerGroupState[] | undefined) => void;
  onResetOffset: (consumerGroup: ConsumerGroup) => void;
} & Pick<
  TableViewProps<ConsumerGroup, (typeof ConsumerGroupColumns)[number]>,
  "isColumnSortable" | "onPageChange" | "onClearAllFilters"
>) {
  const t = useTranslations();
  return (
    <TableView
      variant={TableVariant.compact}
      itemCount={total}
      page={page}
      perPage={perPage}
      onPageChange={onPageChange}
      data={consumerGroups}
      emptyStateNoData={
        <div>{t("ConsumerGroupsTable.no_consumer_groups")}</div>
      }
      emptyStateNoResults={
        <EmptyStateNoMatchFound onClear={onClearAllFilters!} />
      }
      onClearAllFilters={onClearAllFilters}
      ariaLabel={t("ConsumerGroupsTable.title")}
      isFiltered={filterName !== undefined || filterState?.length !== 0}
      columns={ConsumerGroupColumns}
      isColumnSortable={isColumnSortable}
      renderHeader={({ column, key, Th }) => {
        switch (column) {
          case "groupId":
            return (
              <Th key={key} width={30}>
                {t("ConsumerGroupsTable.group_id")}
              </Th>
            );
          case "type":
            return (
              <Th key={key}>
                Type
              </Th>
            );
          case "protocol":
            return (
              <Th key={key}>
                Protocol
              </Th>
            );
          case "state":
            return (
              <Th key={key}>
                {t("ConsumerGroupsTable.state")}{" "}
                <Tooltip
                  content={
                    <RichText>
                      {(tags) =>
                        t.rich("ConsumerGroupsTable.state_tooltip", tags)
                      }
                    </RichText>
                  }
                >
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "lag":
            return (
              <Th key={key}>
                {t("ConsumerGroupsTable.overall_lag")}{" "}
                <Tooltip
                  style={{ whiteSpace: "pre-line" }}
                  content={
                    <RichText>
                      {(tags) =>
                        t.rich("ConsumerGroupsTable.overall_lag_tooltip", tags)
                      }
                    </RichText>
                  }
                >
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "members":
            return (
              <Th key={key}>
                {t("ConsumerGroupsTable.members")}{" "}
                <Tooltip
                  content={
                    <RichText>
                      {(tags) =>
                        t.rich("ConsumerGroupsTable.members_tooltip", tags)
                      }
                    </RichText>
                  }
                >
                  <HelpIcon />
                </Tooltip>
              </Th>
            );
          case "topics":
            return <Th key={key}>{t("ConsumerGroupsTable.topics")}</Th>;
        }
      }}
      renderCell={({ row, column, key, Td }) => {
        switch (column) {
          case "groupId":
            return (
              <Td
                key={key}
                dataLabel={t("ConsumerGroupsTable.group_id")}
              >
                {row.attributes.protocol === "consumer" && row.meta?.describeAvailable
                    ? <Link href={`/kafka/${kafkaId}/groups/${row.id}`}>
                        {row.attributes.groupId}
                      </Link>
                    : <>{row.attributes.groupId}</>
                }
              </Td>
            );
          case "type":
            return (
              <Td key={key} dataLabel={"Type"}>
                {row.attributes.type}
              </Td>
            );
          case "protocol":
            return (
              <Td key={key} dataLabel={"Protocol"}>
                {row.attributes.protocol ?? "-"}
              </Td>
            );
          case "state":
            return (
              <Td key={key} dataLabel={t("ConsumerGroupsTable.state")}>
                {StateLabel[row.attributes.state]?.label}
              </Td>
            );
          case "lag":
            return (
              <Td key={key} dataLabel={t("ConsumerGroupsTable.overall_lag")}>
                <Number
                  value={row.attributes.offsets
                    ?.map((o) => o.lag)
                    // lag values may not be available from API, e.g. when there is an error listing the topic offsets
                    .reduce((acc, v) => (acc ?? NaN) + (v ?? NaN), 0)}
                />
              </Td>
            );
          case "topics":
            const allTopics: Record<string, string | undefined> = {};
            row.attributes.members
              ?.flatMap((m) => m.assignments ?? [])
              .forEach((a) => (allTopics[a.topicName] = a.topicId));
            row.attributes.offsets?.forEach(
              (a) => (allTopics[a.topicName] = a.topicId),
            );
            return (
              <Td key={key} dataLabel={t("ConsumerGroupsTable.topics")}>
                <LabelGroup>
                  {Object.entries(allTopics).map(([topicName, topicId]) => (
                    <LabelLink
                      key={topicName}
                      color={"blue"}
                      href={
                        topicId ? `/kafka/${kafkaId}/topics/${topicId}` : "#"
                      }
                    >
                      {topicName}
                    </LabelLink>
                  ))}
                </LabelGroup>
              </Td>
            );
          case "members":
            return (
              <Td key={key} dataLabel={t("ConsumerGroupsTable.members")}>
                <Number value={row.attributes.members?.length} />
              </Td>
            );
        }
      }}
      renderActions={({ row, ActionsColumn }) => (
        <ActionsColumn
          isDisabled={!hasPrivilege("UPDATE", row) || (row.attributes.protocol !== "consumer") || (!row.meta?.describeAvailable)}
          items={[
            {
              title: t("ConsumerGroupsTable.reset_offset"),
              description:
                row.attributes.state === "EMPTY"
                  ? null
                  : t("ConsumerGroupsTable.reset_offset_description"),
              onClick: () => onResetOffset(row),
            },
          ]}
        />
      )}
      filters={{
        Name: {
          type: "search",
          chips: filterName ? [filterName] : [],
          onSearch: onFilterNameChange,
          onRemoveChip: () => {
            onFilterNameChange(undefined);
          },
          onRemoveGroup: () => {
            onFilterNameChange(undefined);
          },
          validate: () => true,
          errorMessage: "",
        },
        Type: {
          type: "checkbox",
          chips: filterType || [],
          onToggle: (type) => {
            const newType = filterType?.includes(type)
              ? filterType.filter((s) => s !== type)
              : [...filterType!, type];
            onFilterTypeChange(newType);
          },
          onRemoveChip: (type) => {
            const newType = (filterType || []).filter((s) => s !== type);
            onFilterTypeChange(newType);
          },
          onRemoveGroup: () => {
            onFilterTypeChange(undefined);
          },
          options: Object.fromEntries(
            GroupTypeValues.map(v => [v, { label: <>{v}</> }])
          ) as Record<string, { label: React.ReactNode }>,
        },
        State: {
          type: "checkbox",
          chips: filterState || [],
          onToggle: (state) => {
            const newState = filterState?.includes(state)
              ? filterState.filter((s) => s !== state)
              : [...filterState!, state];
            onFilterStateChange(newState);
          },
          onRemoveChip: (state) => {
            const newState = (filterState || []).filter((s) => s !== state);
            onFilterStateChange(newState);
          },
          onRemoveGroup: () => {
            onFilterStateChange(undefined);
          },
          options: StateLabel,
        },
      }}
    />
  );
}
