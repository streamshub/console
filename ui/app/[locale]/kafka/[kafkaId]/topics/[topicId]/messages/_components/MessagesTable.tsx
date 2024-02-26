"use client";
import { Message } from "@/api/messages/schema";
import { AdvancedSearch } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/AdvancedSearch";
import {
  Column,
  columns,
  ColumnsModal,
  useColumnLabels,
} from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/ColumnsModal";
import { NoResultsEmptyState } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/NoResultsEmptyState";
import { Bytes } from "@/components/Bytes";
import { DateTime } from "@/components/DateTime";
import { Number } from "@/components/Number";
import { ResponsiveTable } from "@/components/table";
import {
  Drawer,
  DrawerContent,
  PageSection,
  Text,
  TextContent,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import {
  BaseCellProps,
  InnerScrollContainer,
  OuterScrollContainer,
  TableVariant,
} from "@/libs/patternfly/react-table";
import {
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
} from "@patternfly/react-core";
import { EllipsisVIcon } from "@patternfly/react-icons";
import { useTranslations } from "next-intl";
import { PropsWithChildren, useEffect, useState } from "react";
import { MessageDetails, MessageDetailsProps } from "./MessageDetails";
import { NoDataCell } from "./NoDataCell";
import { UnknownValuePreview } from "./UnknownValuePreview";
import { beautifyUnknownValue, isSameMessage } from "./utils";

const columnWidths: Record<Column, BaseCellProps["width"]> = {
  "offset-partition": 10,
  size: 10,
  key: 15,
  timestamp: 15,
  timestampUTC: 15,
  headers: 20,
  value: undefined,
};

const defaultColumns: Column[] = [
  "offset-partition",
  "timestampUTC",
  "key",
  "value",
];

export type SearchParams = {
  partition?: number;
  query?: {
    value: string;
    where: "headers" | "key" | "value" | "everywhere" | `jq:${string}`;
  };
  from:
    | { type: "timestamp"; value: string }
    | { type: "epoch"; value: number }
    | { type: "offset"; value: number }
    | { type: "latest" };
  until:
    | { type: "limit"; value: number }
    | { type: "live" }
    | { type: "timestamp"; value: string }
    | {
        type: "partition";
        value: number;
      };
};

export type MessageBrowserProps = {
  isRefreshing: boolean;
  selectedMessage?: Message;
  lastUpdated?: Date;
  messages: Message[];
  partitions: number;
  filterLimit: number;
  filterQuery?: string;
  filterWhere?: "key" | "headers" | "value" | `jq:${string}`;
  filterOffset?: number;
  filterEpoch?: number;
  filterTimestamp?: string;
  filterPartition?: number;
  onSearch: (params: SearchParams) => void;
  onSelectMessage: (message: Message) => void;
  onDeselectMessage: () => void;
};

export function MessagesTable({
  isRefreshing,
  selectedMessage,
  messages,
  partitions,
  filterLimit,
  filterQuery,
  filterWhere,
  filterOffset,
  filterEpoch,
  filterTimestamp,
  filterPartition,
  onSearch,
  onSelectMessage,
  onDeselectMessage,
}: MessageBrowserProps) {
  const t = useTranslations("message-browser");
  const columnLabels = useColumnLabels();
  const [showColumnsManagement, setShowColumnsManagement] = useState(false);
  const [defaultTab, setDefaultTab] =
    useState<MessageDetailsProps["defaultTab"]>("value");

  const columnTooltips: Record<Column, any> = {
    "offset-partition": undefined,
    size: (
      <>
        {" "}
        <Tooltip content={t("tooltip.size")}>
          <HelpIcon />
        </Tooltip>
      </>
    ),
    key: undefined,
    timestamp: undefined,
    timestampUTC: undefined,
    headers: undefined,
    value: undefined,
  };

  const [selectedColumns, setSelectedColumns] =
    useState<Column[]>(defaultColumns);

  useEffect(() => {
    const v = localStorage.getItem("message-browser-columns");
    if (v) {
      try {
        const pv = JSON.parse(v);
        if (Array.isArray(pv)) {
          setSelectedColumns(pv);
        }
      } catch {}
    }
  }, []);

  return (
    <PageSection
      isFilled={true}
      hasOverflowScroll={true}
      style={{ height: "calc(100vh - 170px - 70px)" }}
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
              partitions={partitions}
              filterLimit={filterLimit}
              filterQuery={filterQuery}
              filterWhere={filterWhere}
              filterOffset={filterOffset}
              filterEpoch={filterEpoch}
              filterTimestamp={filterTimestamp}
              filterPartition={filterPartition}
              onSearch={onSearch}
              onColumnManagement={() => setShowColumnsManagement(true)}
            />
            <InnerScrollContainer>
              <ResponsiveTable
                variant={TableVariant.compact}
                ariaLabel={t("table_aria_label")}
                columns={columns.filter((c) => selectedColumns.includes(c))}
                data={messages}
                expectedLength={messages.length}
                renderHeader={({ colIndex, column, Th, key }) => (
                  <Th
                    key={key}
                    width={columnWidths[column]}
                    isStickyColumn={colIndex < 2}
                    hasRightBorder={colIndex === 1}
                    stickyMinWidth={colIndex === 1 ? "100px" : undefined}
                    stickyLeftOffset={colIndex === 1 ? "187px" : undefined}
                    modifier={"nowrap"}
                    sort={
                      column === "timestamp" ||
                      column === "timestampUTC" ||
                      column === "offset-partition"
                        ? {
                            columnIndex: colIndex,
                            sortBy: {
                              index: colIndex,
                              direction:
                                filterOffset || filterTimestamp || filterEpoch
                                  ? "asc"
                                  : "desc",
                            },
                          }
                        : undefined
                    }
                  >
                    {columnLabels[column]}
                    {columnTooltips[column] ?? ""}
                  </Th>
                )}
                renderCell={({ column, row, colIndex, Td, key }) => {
                  const empty = (
                    <NoDataCell columnLabel={columnLabels[column]} />
                  );

                  function Cell({ children }: PropsWithChildren) {
                    return (
                      <Td
                        key={key}
                        dataLabel={columnLabels[column]}
                        isStickyColumn={colIndex < 2}
                        hasRightBorder={colIndex === 1}
                        stickyMinWidth={colIndex === 1 ? "100px" : undefined}
                        stickyLeftOffset={colIndex === 1 ? "187px" : undefined}
                        modifier={"nowrap"}
                      >
                        {children}
                      </Td>
                    );
                  }

                  switch (column) {
                    case "offset-partition":
                      return (
                        <Cell>
                          <Number value={row.attributes.offset} />
                          <TextContent>
                            <Text component={"small"}>
                              Partition{" "}
                              <Number value={row.attributes.partition} />
                            </Text>
                          </TextContent>
                        </Cell>
                      );
                    case "size":
                      return (
                        <Cell>
                          <Bytes value={row.attributes.size} />
                        </Cell>
                      );
                    case "timestamp":
                      return (
                        <Cell>
                          <DateTime
                            value={row.attributes.timestamp}
                            dateStyle={"short"}
                            timeStyle={"medium"}
                          />
                        </Cell>
                      );
                    case "timestampUTC":
                      return (
                        <Cell>
                          <DateTime
                            value={row.attributes.timestamp}
                            dateStyle={"short"}
                            timeStyle={"medium"}
                            tz={"UTC"}
                          />
                        </Cell>
                      );
                    case "key":
                      return (
                        <Cell>
                          {row.attributes.key ? (
                            <UnknownValuePreview
                              value={row.attributes.key}
                              highlight={filterQuery}
                              onClick={() => {
                                setDefaultTab("key");
                                onSelectMessage(row);
                              }}
                            />
                          ) : (
                            empty
                          )}
                        </Cell>
                      );
                    case "headers":
                      return (
                        <Cell>
                          {Object.keys(row.attributes.headers).length > 0 ? (
                            <UnknownValuePreview
                              value={beautifyUnknownValue(
                                JSON.stringify(row.attributes.headers),
                              )}
                              highlight={filterQuery}
                              onClick={() => {
                                setDefaultTab("headers");
                                onSelectMessage(row);
                              }}
                            />
                          ) : (
                            empty
                          )}
                        </Cell>
                      );
                    case "value":
                      return (
                        <Cell>
                          {row.attributes.value ? (
                            <UnknownValuePreview
                              value={row.attributes.value}
                              highlight={filterQuery}
                              onClick={() => {
                                setDefaultTab("value");
                                onSelectMessage(row);
                              }}
                            />
                          ) : (
                            empty
                          )}
                        </Cell>
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
  filterQuery,
  filterWhere,
  filterEpoch,
  filterTimestamp,
  filterOffset,
  filterPartition,
  partitions,
  filterLimit,
  onSearch,
  onColumnManagement,
}: Pick<
  MessageBrowserProps,
  | "filterQuery"
  | "filterWhere"
  | "filterEpoch"
  | "filterTimestamp"
  | "filterOffset"
  | "filterPartition"
  | "partitions"
  | "filterLimit"
  | "onSearch"
> & {
  onColumnManagement: () => void;
}) {
  const toolbarBreakpoint = "md";

  function onClearAllFilters() {}

  // const handleQueryChange = useDebouncedCallback((value: string) => {
  //   onQueryChange(value);
  // }, 300);

  return (
    <Toolbar
      clearAllFilters={onClearAllFilters}
      collapseListedFiltersBreakpoint={toolbarBreakpoint}
    >
      <ToolbarContent>
        <ToolbarItem
          variant={"search-filter"}
          widths={{ default: "calc(100% - 58px)" }}
        >
          <AdvancedSearch
            filterQuery={filterQuery}
            filterWhere={filterWhere}
            filterEpoch={filterEpoch}
            filterOffset={filterOffset}
            filterPartition={filterPartition}
            filterTimestamp={filterTimestamp}
            filterLimit={filterLimit}
            partitions={partitions}
            onSearch={onSearch}
          />
        </ToolbarItem>

        <ToolbarItem>
          <Dropdown
            popperProps={{ position: "right" }}
            isOpen={false}
            onOpenChange={(isOpen: boolean) => {}}
            toggle={(toggleRef) => (
              <MenuToggle
                ref={toggleRef}
                isExpanded={false}
                onClick={() => {}}
                variant="plain"
                aria-label="Table options"
              >
                <EllipsisVIcon aria-hidden="true" />
              </MenuToggle>
            )}
          >
            <DropdownList>
              <DropdownItem>Action</DropdownItem>
              <DropdownItem
                // Prevent the default onClick functionality for example purposes
                onClick={() => onColumnManagement()}
              >
                Manage columns
              </DropdownItem>
            </DropdownList>
          </Dropdown>
        </ToolbarItem>
      </ToolbarContent>
    </Toolbar>
  );
}

export function MessagesTableSkeleton({
  filterLimit,
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
