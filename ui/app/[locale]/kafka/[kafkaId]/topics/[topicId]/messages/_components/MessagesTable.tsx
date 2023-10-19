import { Message } from "@/api/messages";
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
  Drawer,
  DrawerContent,
  PageSection,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { FilterIcon } from "@/libs/patternfly/react-icons";
import type { BaseCellProps } from "@/libs/patternfly/react-table";
import {
  InnerScrollContainer,
  OuterScrollContainer,
} from "@/libs/patternfly/react-table";
import { ToolbarToggleGroup } from "@patternfly/react-core";
import { TableVariant } from "@patternfly/react-table";
import { useTranslations } from "next-intl";
import { useMemo, useState } from "react";
import { LimitSelector } from "./LimitSelector";
import { MessageDetails, MessageDetailsProps } from "./MessageDetails";
import { NoDataCell } from "./NoDataCell";
import { UnknownValuePreview } from "./UnknownValuePreview";
import { beautifyUnknownValue, isSameMessage } from "./utils";

const columns = [
  // "partition",
  "offset",
  "timestamp",
  "key",
  // "headers",
  "value",
] as const;

const columnWidths: BaseCellProps["width"][] = [10, 15, 10, undefined];

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
  const [defaultTab, setDefaultTab] =
    useState<MessageDetailsProps["defaultTab"]>("value");

  const columnLabels: { [key in (typeof columns)[number]]: string } = useMemo(
    () =>
      ({
        // partition: t("field.partition"),
        offset: t("field.offset"),
        timestamp: t("field.timestamp"),
        key: t("field.key"),
        value: t("field.value"),
        headers: t("field.headers"),
      }) as const,
    [t],
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
            />
            <InnerScrollContainer>
              <ResponsiveTable
                variant={TableVariant.compact}
                ariaLabel={t("table_aria_label")}
                columns={columns}
                data={messages}
                expectedLength={messages.length}
                renderHeader={({ column, Th, key }) => (
                  <Th key={key}>{columnLabels[column]}</Th>
                )}
                renderCell={({ column, row, colIndex, Td, key }) => (
                  <Td
                    key={key}
                    dataLabel={columnLabels[column]}
                    width={columnWidths[colIndex]}
                  >
                    {(() => {
                      const empty = (
                        <NoDataCell columnLabel={columnLabels[column]} />
                      );
                      switch (column) {
                        // case "partition":
                        //   return row.attributes.partition;
                        case "offset":
                          return <Number value={row.attributes.offset} />;
                        case "timestamp":
                          return (
                            <DateTime
                              value={row.attributes.timestamp}
                              dateStyle={"short"}
                              timeStyle={"medium"}
                            />
                          );
                        case "key":
                          return row.attributes.key ? (
                            <UnknownValuePreview
                              value={row.attributes.key}
                              truncateAt={40}
                            />
                          ) : (
                            empty
                          );
                        // case "headers":
                        //   return Object.keys(row.attributes.headers)
                        //     .length > 0 ? (
                        //     <UnknownValuePreview
                        //       value={beautifyUnknownValue(
                        //         JSON.stringify(row.attributes.headers),
                        //       )}
                        //       onClick={() => {
                        //         setDefaultTab("headers");
                        //         selectMessage(row);
                        //       }}
                        //     />
                        //   ) : (
                        //     empty
                        //   );
                        case "value":
                          return row.attributes.value ? (
                            <UnknownValuePreview
                              value={beautifyUnknownValue(
                                row.attributes.value || "",
                              )}
                              onClick={() => {
                                setDefaultTab("value");
                                onSelectMessage(row);
                              }}
                            />
                          ) : (
                            empty
                          );
                      }
                    })()}
                  </Td>
                )}
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
>) {
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
