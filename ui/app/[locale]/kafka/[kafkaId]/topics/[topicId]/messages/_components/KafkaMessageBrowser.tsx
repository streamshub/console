"use client";
import { Message } from "@/api/messages";
import { NoResultsEmptyState } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/NoResultsEmptyState";
import { Loading } from "@/components/Loading";
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
import { TableVariant } from "@patternfly/react-table";
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
  // "partition",
  "offset",
  "timestamp",
  "key",
  // "headers",
  "value",
] as const;

const columnWidths: BaseCellProps["width"][] = [10, 20, 10, undefined];

export type KafkaMessageBrowserProps = {
  isFirstLoad: boolean;
  isNoData: boolean;
  isRefreshing: boolean;
  requiresSearch: boolean;
  selectedMessage: Message | undefined;
  lastUpdated: Date | undefined;
  messages: Message[];
  partitions: number;
  offsetMin: number | undefined;
  offsetMax: number | undefined;
  partition: number | undefined;
  limit: number;
  filterOffset: number | undefined;
  filterEpoch: number | undefined;
  filterTimestamp: string | undefined;
  setPartition: (value: number | undefined) => void;
  setOffset: (value: number | undefined) => void;
  setTimestamp: (value: string | undefined) => void;
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
  messages,
  partitions,
  offsetMin,
  offsetMax,
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
        // partition: t("field.partition"),
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
          aria-label={t("title")}
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
                <Toolbar data-testid={"message-browser-toolbar"}>
                  <ToolbarContent>
                    <ToolbarToggleGroup
                      toggleIcon={<FilterIcon />}
                      breakpoint="md"
                    >
                      <ToolbarGroup variant="filter-group">
                        <ToolbarItem>
                          <PartitionSelector
                            value={partition}
                            partitions={partitions}
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
                      {partition !== undefined &&
                        messages.length > 0 &&
                        offsetMin !== undefined &&
                        offsetMax !== undefined && (
                          <OffsetRange min={offsetMin} max={offsetMax - 1} />
                        )}
                    </ToolbarGroup>
                  </ToolbarContent>
                </Toolbar>
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
                              return row.attributes.offset;
                            case "timestamp":
                              return row.attributes.timestamp
                                ? format.dateTime(
                                    parseISO(row.attributes.timestamp),
                                    {
                                      dateStyle: "long",
                                      timeStyle: "long",
                                    },
                                  )
                                : empty;
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
