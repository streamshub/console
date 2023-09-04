"use client";
import { Message, MessageApiResponse } from "@/api/topics";
import { Loading } from "@/components/loading/loading";
import { NoResultsEmptyState } from "@/components/messageBrowser/NoResultsEmptyState";
import { RefreshButton } from "@/components/refreshButton/refreshButton";
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
import { FilterIcon, SearchIcon } from "@/libs/patternfly/react-icons";
import type { BaseCellProps } from "@/libs/patternfly/react-table";
import {
  InnerScrollContainer,
  OuterScrollContainer,
} from "@/libs/patternfly/react-table";
import { parseISO } from "date-fns";
import { useFormatter, useTranslations } from "next-intl";
import { useMemo, useState } from "react";
import { FilterGroup } from "./FilterGroup";
import { LimitSelector } from "./LimitSelector";
import { MessageDetails, MessageDetailsProps } from "./MessageDetails";
import { NoDataCell } from "./NoDataCell";
import { NoDataEmptyState } from "./NoDataEmptyState";
import { OffsetRange } from "./OffsetRange";
import { PartitionSelector } from "./PartitionSelector";
import { UnknownValuePreview } from "./UnknownValuePreview";
import { beautifyUnknownValue, isSameMessage } from "./utils";

const columns = [
  "partition",
  "offset",
  "timestamp",
  "key",
  "headers",
  "value",
] as const;

const columnWidths: BaseCellProps["width"][] = [10, 10, 15, 10, undefined, 30];

export type KafkaMessageBrowserProps = {
  isFirstLoad: boolean;
  isNoData: boolean;
  isRefreshing: boolean;
  requiresSearch: boolean;
  selectedMessage: Message | undefined;
  lastUpdated: Date | undefined;
  response: MessageApiResponse | undefined;
  partition: number | undefined;
  limit: number;
  filterOffset: number | undefined;
  filterEpoch: number | undefined;
  filterTimestamp: DateIsoString | undefined;
  setPartition: (value: number | undefined) => void;
  setOffset: (value: number | undefined) => void;
  setTimestamp: (value: DateIsoString | undefined) => void;
  setEpoch: (value: number | undefined) => void;
  setLatest: () => void;
  setLimit: (value: number) => void;
  refresh: () => void;
  selectMessage: (message: Message) => void;
  deselectMessage: () => void;
};
export function KafkaMessageBrowser({
  isFirstLoad,
  isNoData,
  isRefreshing,
  requiresSearch,
  selectedMessage,
  response,
  partition,
  limit,
  filterOffset,
  filterEpoch,
  filterTimestamp,
  setPartition,
  setOffset,
  setTimestamp,
  setEpoch,
  setLatest,
  setLimit,
  refresh,
  selectMessage,
  deselectMessage,
}: KafkaMessageBrowserProps) {
  const t = useTranslations("message-browser");
  const format = useFormatter();
  const [defaultTab, setDefaultTab] =
    useState<MessageDetailsProps["defaultTab"]>("value");

  const columnLabels: { [key in (typeof columns)[number]]: string } = useMemo(
    () =>
      ({
        partition: t("field.partition"),
        offset: t("field.offset"),
        timestamp: t("field.timestamp"),
        key: t("field.key"),
        value: t("field.value"),
        headers: t("field.headers"),
      }) as const,
    [t],
  );

  switch (true) {
    case isFirstLoad:
      return <Loading />;
    case isNoData:
      return <NoDataEmptyState onRefresh={refresh} />;
    default:
      return (
        <PageSection
          isFilled={true}
          hasOverflowScroll={true}
          aria-label={"TODO"}
        >
          <Drawer isInline={true} isExpanded={selectedMessage !== undefined}>
            <DrawerContent
              panelContent={
                <MessageDetails
                  message={selectedMessage}
                  defaultTab={defaultTab}
                  onClose={deselectMessage}
                />
              }
            >
              <OuterScrollContainer>
                <Toolbar
                  className={"mas-KafkaMessageBrowser-Toolbar"}
                  data-testid={"message-browser-toolbar"}
                >
                  <ToolbarContent>
                    <ToolbarToggleGroup
                      toggleIcon={<FilterIcon />}
                      breakpoint="2xl"
                    >
                      <ToolbarGroup variant="filter-group">
                        <ToolbarItem>
                          <PartitionSelector
                            value={partition}
                            partitions={response?.partitions || 0}
                            onChange={setPartition}
                            isDisabled={isRefreshing}
                          />
                        </ToolbarItem>
                      </ToolbarGroup>
                      <ToolbarGroup variant="filter-group">
                        <FilterGroup
                          isDisabled={isRefreshing}
                          offset={filterOffset}
                          epoch={filterEpoch}
                          timestamp={filterTimestamp}
                          onOffsetChange={setOffset}
                          onTimestampChange={setTimestamp}
                          onEpochChange={setEpoch}
                          onLatest={setLatest}
                        />
                      </ToolbarGroup>
                      <ToolbarGroup>
                        <LimitSelector
                          value={limit}
                          onChange={setLimit}
                          isDisabled={isRefreshing}
                        />
                      </ToolbarGroup>
                    </ToolbarToggleGroup>
                    <ToolbarGroup>
                      <ToolbarItem>
                        <Button
                          variant={"plain"}
                          isDisabled={!requiresSearch || isRefreshing}
                          aria-label={t("search_button_label")}
                          onClick={refresh}
                        >
                          <SearchIcon />
                        </Button>
                      </ToolbarItem>
                      <ToolbarItem>
                        <RefreshButton
                          onClick={refresh}
                          isRefreshing={isRefreshing}
                          isDisabled={requiresSearch}
                        />
                      </ToolbarItem>
                    </ToolbarGroup>
                    <ToolbarGroup>
                      {response?.filter.partition !== undefined &&
                        response?.messages.length > 0 && (
                          <OffsetRange
                            min={response?.offsetMin || 0}
                            max={response?.offsetMax || 0}
                          />
                        )}
                    </ToolbarGroup>
                  </ToolbarContent>
                </Toolbar>
                <InnerScrollContainer>
                  <ResponsiveTable
                    ariaLabel={t("table_aria_label")}
                    columns={columns}
                    data={response?.messages}
                    expectedLength={response?.messages?.length}
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
                            case "partition":
                              return row.partition;
                            case "offset":
                              return row.offset;
                            case "timestamp":
                              return row.timestamp
                                ? format.dateTime(parseISO(row.timestamp), {
                                    dateStyle: "long",
                                    timeStyle: "long",
                                  })
                                : empty;
                            case "key":
                              return row.key ? (
                                <UnknownValuePreview
                                  value={row.key}
                                  truncateAt={40}
                                />
                              ) : (
                                empty
                              );
                            case "headers":
                              return Object.keys(row.headers).length > 0 ? (
                                <UnknownValuePreview
                                  value={beautifyUnknownValue(
                                    JSON.stringify(row.headers),
                                  )}
                                  onClick={() => {
                                    setDefaultTab("headers");
                                    selectMessage(row);
                                  }}
                                />
                              ) : (
                                empty
                              );
                            case "value":
                              return row.value ? (
                                <UnknownValuePreview
                                  value={beautifyUnknownValue(row.value || "")}
                                  onClick={() => {
                                    setDefaultTab("value");
                                    selectMessage(row);
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
                      selectMessage(row);
                    }}
                  >
                    <NoResultsEmptyState
                      onReset={() => {
                        setLatest();
                        setPartition(undefined);
                        refresh();
                      }}
                    />
                  </ResponsiveTable>
                </InnerScrollContainer>
              </OuterScrollContainer>
            </DrawerContent>
          </Drawer>
        </PageSection>
      );
  }
}
