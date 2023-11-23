import { Message } from "@/api/messages/schema";
import {
  Column,
  columnLabels,
  columns,
  ColumnsModal,
} from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/ColumnsModal";
import { FilterGroup } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/FilterGroup";
import { NoResultsEmptyState } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/NoResultsEmptyState";
import { PartitionSelector } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/PartitionSelector";
import {
  RefreshInterval,
  RefreshSelector,
} from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/RefreshSelector";
import { DateTime } from "@/components/DateTime";
import { Number } from "@/components/Number";
import { ResponsiveTable } from "@/components/table";
import {
  Button,
  Drawer,
  DrawerContent,
  PageSection,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  ToolbarItem,
  ToolbarToggleGroup,
} from "@/libs/patternfly/react-core";
import { FilterIcon } from "@/libs/patternfly/react-icons";
import {
  InnerScrollContainer,
  OuterScrollContainer,
  TableVariant,
} from "@/libs/patternfly/react-table";
import { BaseCellProps } from "@patternfly/react-table";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { LimitSelector } from "./LimitSelector";
import { MessageDetails, MessageDetailsProps } from "./MessageDetails";
import { NoDataCell } from "./NoDataCell";
import { UnknownValuePreview } from "./UnknownValuePreview";
import { beautifyUnknownValue, isSameMessage } from "./utils";

const columnWidths: Record<Column, BaseCellProps["width"]> = {
  offset: 10,
  key: 15,
  timestamp: 15,
  timestampUTC: 15,
  headers: 20,
  partitions: 10,
  value: undefined,
};

const defaultColumns = ["offset", "timestamp", "key", "value"];

export type MessageBrowserProps = {
  isRefreshing: boolean;
  refreshInterval: RefreshInterval;
  selectedMessage: Message | undefined;
  lastUpdated: Date | undefined;
  messages: Message[];
  partitions: number;
  partition: number | undefined;
  limit: number;
  filterOffset: number | undefined;
  filterEpoch: number | undefined;
  filterTimestamp: string | undefined;
  onPartitionChange: (value: number | undefined) => void;
  onOffsetChange: (value: number | undefined) => void;
  onTimestampChange: (value: string | undefined) => void;
  onEpochChange: (value: number | undefined) => void;
  onLatest: () => void;
  onLimitChange: (value: number) => void;
  onRefresh: () => void;
  onRefreshInterval: (interval: RefreshInterval) => void;
  onSelectMessage: (message: Message) => void;
  onDeselectMessage: () => void;
};

export function MessagesTable({
  isRefreshing,
  selectedMessage,
  messages,
  partitions,
  partition,
  limit,
  filterOffset,
  filterEpoch,
  filterTimestamp,
  refreshInterval,
  onPartitionChange,
  onOffsetChange,
  onTimestampChange,
  onEpochChange,
  onLatest,
  onLimitChange,
  onRefresh,
  onRefreshInterval,
  onSelectMessage,
  onDeselectMessage,
}: MessageBrowserProps) {
  const t = useTranslations("message-browser");
  const [showColumnsManagement, setShowColumnsManagement] = useState(false);
  const [defaultTab, setDefaultTab] =
    useState<MessageDetailsProps["defaultTab"]>("value");
  const previouslySelectedColumns = (() => {
    const v = localStorage.getItem("message-browser-columns");
    if (v) {
      try {
        const pv = JSON.parse(v);
        if (Array.isArray(pv)) {
          return pv;
        }
      } catch {}
    }
    return defaultColumns;
  })();

  const [selectedColumns, setSelectedColumns] = useState<Column[]>(
    previouslySelectedColumns,
  );

  return (
    <PageSection
      isFilled={true}
      hasOverflowScroll={true}
      aria-label={t("title")}
    >
      <Drawer isInline={true} isExpanded={selectedMessage !== undefined}>
        <DrawerContent
          panelContent={
            <MessageDetails
              message={selectedMessage}
              defaultTab={defaultTab}
              onClose={onDeselectMessage}
            />
          }
        >
          <OuterScrollContainer>
            <MessagesTableToolbar
              isRefreshing={isRefreshing}
              refreshInterval={refreshInterval}
              partitions={partitions}
              partition={partition}
              limit={limit}
              filterOffset={filterOffset}
              filterEpoch={filterEpoch}
              filterTimestamp={filterTimestamp}
              onPartitionChange={onPartitionChange}
              onOffsetChange={onOffsetChange}
              onTimestampChange={onTimestampChange}
              onEpochChange={onEpochChange}
              onLatest={onLatest}
              onLimitChange={onLimitChange}
              onRefresh={onRefresh}
              onRefreshInterval={onRefreshInterval}
              onColumnManagement={() => setShowColumnsManagement(true)}
            />
            <InnerScrollContainer>
              <ResponsiveTable
                variant={TableVariant.compact}
                ariaLabel={t("table_aria_label")}
                columns={columns.filter((c) => selectedColumns.includes(c))}
                data={messages}
                expectedLength={messages.length}
                renderHeader={({ column, Th, key }) => (
                  <Th key={key} width={columnWidths[column]}>
                    {columnLabels[column]}
                  </Th>
                )}
                renderCell={({ column, row, colIndex, Td, key }) => {
                  const empty = (
                    <NoDataCell columnLabel={columnLabels[column]} />
                  );
                  switch (column) {
                    case "offset":
                      return (
                        <Td key={key} dataLabel={columnLabels[column]}>
                          <Number value={row.attributes.offset} />
                        </Td>
                      );
                    case "partitions":
                      return (
                        <Td key={key} dataLabel={columnLabels[column]}>
                          <Number value={row.attributes.partition} />
                        </Td>
                      );
                    case "timestamp":
                      return (
                        <Td key={key} dataLabel={columnLabels[column]}>
                          <DateTime
                            value={row.attributes.timestamp}
                            dateStyle={"short"}
                            timeStyle={"medium"}
                          />
                        </Td>
                      );
                    case "timestampUTC":
                      return (
                        <Td key={key} dataLabel={columnLabels[column]}>
                          <DateTime
                            value={row.attributes.timestamp}
                            dateStyle={"short"}
                            timeStyle={"medium"}
                            tz={"UTC"}
                          />
                        </Td>
                      );
                    case "key":
                      return (
                        <Td key={key} dataLabel={columnLabels[column]}>
                          {row.attributes.key ? (
                            <UnknownValuePreview
                              value={row.attributes.key}
                              truncateAt={40}
                            />
                          ) : (
                            empty
                          )}
                        </Td>
                      );
                    case "headers":
                      return (
                        <Td key={key} dataLabel={columnLabels[column]}>
                          {Object.keys(row.attributes.headers).length > 0 ? (
                            <UnknownValuePreview
                              value={beautifyUnknownValue(
                                JSON.stringify(row.attributes.headers),
                              )}
                              onClick={() => {
                                setDefaultTab("headers");
                                onSelectMessage(row);
                              }}
                            />
                          ) : (
                            empty
                          )}
                        </Td>
                      );
                    case "value":
                      return row.attributes.value ? (
                        <UnknownValuePreview
                          value={row.attributes.value}
                          truncateAt={149}
                          onClick={() => {
                            setDefaultTab("value");
                            onSelectMessage(row);
                          }}
                        />
                      ) : (
                        empty
                      );
                  }
                }}
                isRowSelected={({ row }) =>
                  selectedMessage !== undefined &&
                  isSameMessage(row, selectedMessage)
                }
                onRowClick={({ row }) => {
                  setDefaultTab("value");
                  onSelectMessage(row);
                }}
              >
                <NoResultsEmptyState />
              </ResponsiveTable>
            </InnerScrollContainer>
          </OuterScrollContainer>
        </DrawerContent>
      </Drawer>
      <ColumnsModal
        isOpen={showColumnsManagement}
        selectedColumns={selectedColumns}
        onConfirm={(columns) => {
          setSelectedColumns(columns);
          localStorage.setItem(
            "message-browser-columns",
            JSON.stringify(columns),
          );
          setShowColumnsManagement(false);
        }}
        onCancel={() => setShowColumnsManagement(false)}
      />
    </PageSection>
  );
}

export function MessagesTableToolbar({
  isRefreshing,
  filterEpoch,
  filterTimestamp,
  filterOffset,
  partition,
  partitions,
  limit,
  refreshInterval,
  onEpochChange,
  onTimestampChange,
  onOffsetChange,
  onPartitionChange,
  onLimitChange,
  onLatest,
  onRefresh,
  onRefreshInterval,
  onColumnManagement,
}: Pick<
  MessageBrowserProps,
  | "isRefreshing"
  | "filterEpoch"
  | "filterTimestamp"
  | "filterOffset"
  | "partition"
  | "partitions"
  | "limit"
  | "refreshInterval"
  | "onEpochChange"
  | "onTimestampChange"
  | "onOffsetChange"
  | "onPartitionChange"
  | "onLimitChange"
  | "onLatest"
  | "onRefresh"
  | "onRefreshInterval"
> & {
  onColumnManagement: () => void;
}) {
  const toolbarBreakpoint = "md";

  function onClearAllFilters() {}

  return (
    <Toolbar
      clearAllFilters={onClearAllFilters}
      collapseListedFiltersBreakpoint={toolbarBreakpoint}
    >
      <ToolbarContent>
        <ToolbarItem
          visibility={{
            default: "hidden",
            [toolbarBreakpoint]: "visible",
          }}
        >
          <FilterGroup
            isDisabled={isRefreshing}
            offset={filterOffset}
            epoch={filterEpoch}
            timestamp={filterTimestamp}
            onOffsetChange={onOffsetChange}
            onTimestampChange={onTimestampChange}
            onEpochChange={onEpochChange}
            onLatest={onLatest}
          />
        </ToolbarItem>
        <ToolbarItem
          visibility={{
            default: "hidden",
            [toolbarBreakpoint]: "visible",
          }}
        >
          <PartitionSelector
            value={partition}
            partitions={partitions}
            onChange={onPartitionChange}
            isDisabled={isRefreshing}
          />
        </ToolbarItem>

        <ToolbarToggleGroup
          toggleIcon={<FilterIcon />}
          breakpoint={toolbarBreakpoint}
          visibility={{
            default: "visible",
            [toolbarBreakpoint]: "hidden",
          }}
        >
          <ToolbarItem>
            <FilterGroup
              isDisabled={isRefreshing}
              offset={filterOffset}
              epoch={filterEpoch}
              timestamp={filterTimestamp}
              onOffsetChange={onOffsetChange}
              onTimestampChange={onTimestampChange}
              onEpochChange={onEpochChange}
              onLatest={onLatest}
            />
          </ToolbarItem>
          <ToolbarItem>
            <PartitionSelector
              value={partition}
              partitions={partitions}
              onChange={onPartitionChange}
              isDisabled={isRefreshing}
            />
          </ToolbarItem>
        </ToolbarToggleGroup>
        {/* icon buttons */}
        <ToolbarGroup variant="icon-button-group">
          <ToolbarItem>
            <RefreshSelector
              isRefreshing={isRefreshing}
              refreshInterval={refreshInterval}
              onClick={onRefresh}
              onChange={onRefreshInterval}
            />
          </ToolbarItem>
        </ToolbarGroup>

        <ToolbarItem>
          <Button onClick={onColumnManagement} variant={"link"}>
            Manage columns
          </Button>
        </ToolbarItem>

        <ToolbarGroup align={{ default: "alignRight" }}>
          <LimitSelector
            value={limit}
            onChange={onLimitChange}
            isDisabled={isRefreshing}
          />
        </ToolbarGroup>
      </ToolbarContent>
    </Toolbar>
  );
}

export function MessagesTableSkeleton({
  partition,
  limit,
  filterTimestamp,
  filterOffset,
  filterEpoch,
}: Pick<
  MessageBrowserProps,
  "partition" | "limit" | "filterTimestamp" | "filterOffset" | "filterEpoch"
>) {
  const t = useTranslations("message-browser");
  return (
    <PageSection
      isFilled={true}
      hasOverflowScroll={true}
      aria-label={t("title")}
    >
      <MessagesTableToolbar
        isRefreshing={true}
        refreshInterval={undefined}
        partitions={1}
        partition={partition}
        limit={limit}
        filterOffset={filterOffset}
        filterEpoch={filterEpoch}
        filterTimestamp={filterTimestamp}
        onPartitionChange={() => {}}
        onOffsetChange={() => {}}
        onTimestampChange={() => {}}
        onEpochChange={() => {}}
        onLatest={() => {}}
        onLimitChange={() => {}}
        onRefresh={() => {}}
        onRefreshInterval={() => {}}
        onColumnManagement={() => {}}
      />
      <ResponsiveTable
        variant={TableVariant.compact}
        ariaLabel={t("table_aria_label")}
        columns={columns}
        data={undefined}
        expectedLength={20}
        renderCell={() => <div></div>}
        renderHeader={() => <div></div>}
      />
    </PageSection>
  );
}
