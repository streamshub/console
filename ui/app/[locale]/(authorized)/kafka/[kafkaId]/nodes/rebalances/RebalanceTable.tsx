import { TableView, TableViewProps } from "@/components/Table";
import { useTranslations } from "next-intl";
import {
  RebalanceList,
  RebalanceMode,
  RebalanceStatus,
} from "@/api/rebalance/schema";
import {
  Flex,
  FlexItem,
  Icon,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Badge,
  Popover,
  List,
  ListItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import {
  CheckIcon,
  ExclamationCircleIcon,
  PauseCircleIcon,
  PendingIcon,
  OutlinedClockIcon,
  HelpIcon,
} from "@/libs/patternfly/react-icons";
import { Th } from "@/libs/patternfly/react-table";
import Link from "next/link";
import React, { ReactNode } from "react";
import { EmptyStateNoMatchFound } from "@/components/Table/EmptyStateNoMatchFound";
import { EmptyStateNoKafkaRebalance } from "./EmptyStateNoKafkaRebalance";
import Image from "next/image";
import { DateTime } from "@/components/Format/DateTime";
import RichText from "@/components/RichText";
import { hasPrivilege } from "@/utils/privileges";
import { ThSortType } from "@patternfly/react-table/dist/esm/components/Table/base/types";

export const RebalanceTableColumns = ["name", "status", "lastUpdated"] as const;

export type RebalanceTableColumn = (typeof RebalanceTableColumns)[number];

const StatusLabel: Record<RebalanceStatus, { label: ReactNode }> = {
  New: {
    label: (
      <Tooltip content="New rebalance initiated">
        <span>
          <Icon>
            <ExclamationCircleIcon />
          </Icon>
          &nbsp; New
        </span>
      </Tooltip>
    ),
  },
  PendingProposal: {
    label: (
      <Tooltip content="Optimization proposal not generated">
        <span>
          <Icon>
            <PendingIcon />
          </Icon>
          &nbsp;PendingProposal
        </span>
      </Tooltip>
    ),
  },
  ProposalReady: {
    label: (
      <Tooltip content="Optimization proposal is ready for approval">
        <span>
          <Icon>
            <CheckIcon />
          </Icon>
          &nbsp;ProposalReady
        </span>
      </Tooltip>
    ),
  },
  Stopped: {
    label: (
      <Tooltip content="Rebalance stopped">
        <span>
          <Icon>
            <Image
              src={"/stop-icon.svg"}
              alt="stop icon"
              width={100}
              height={100}
            />
          </Icon>
          &nbsp;Stopped
        </span>
      </Tooltip>
    ),
  },
  Rebalancing: {
    label: (
      <Tooltip content="Rebalance in progress">
        <span>
          <Icon>
            <PendingIcon />
          </Icon>
          &nbsp;Rebalancing
        </span>
      </Tooltip>
    ),
  },
  NotReady: {
    label: (
      <Tooltip content="Error occurred with the rebalance">
        <span>
          <Icon>
            <OutlinedClockIcon />
          </Icon>
          &nbsp;NotReady
        </span>
      </Tooltip>
    ),
  },
  Ready: {
    label: (
      <Tooltip content="Rebalance complete">
        <span>
          <Icon>
            <CheckIcon />
          </Icon>
          &nbsp;Ready
        </span>
      </Tooltip>
    ),
  },
  ReconciliationPaused: {
    label: (
      <Tooltip content="Rebalance is paused">
        <span>
          <Icon>
            <PauseCircleIcon />
          </Icon>
          &nbsp;ReconciliationPaused
        </span>
      </Tooltip>
    ),
  },
};

function statusLabel(status: RebalanceStatus | null): { label: ReactNode } {
  return status
    ? StatusLabel[status]
    : {
        label: (
          <Tooltip content="Rebalance status is missing">
            <span>
              <Icon>
                <PauseCircleIcon />
              </Icon>
              &nbsp;Unknown
            </span>
          </Tooltip>
        ),
      };
}

const ModeLabel: Record<RebalanceMode, { label: ReactNode }> = {
  full: { label: <> Full</> },
  "add-brokers": { label: <>Add</> },
  "remove-brokers": { label: <>Remove</> },
};

export type RebalanceTableProps = {
  baseurl: string;
  rebalanceList: RebalanceList[] | undefined;
  rebalancesCount: number;
  page: number;
  perPage: number;
  onApprove: (rebalanceName: RebalanceList) => void;
  onStop: (rebalanceName: RebalanceList) => void;
  onRefresh: (rebalanceName: RebalanceList) => void;
  filterName: string | undefined;
  filterStatus: RebalanceStatus[] | undefined;
  filterMode: RebalanceMode[] | undefined;
  onFilterNameChange: (name: string | undefined) => void;
  onFilterModeChange: (mode: RebalanceMode[] | undefined) => void;
  onFilterStatusChange: (status: RebalanceStatus[] | undefined) => void;
  sortProvider: (col: RebalanceTableColumn) => ThSortType | undefined;
} & Pick<
  TableViewProps<RebalanceList, (typeof RebalanceTableColumns)[number]>,
  "onPageChange" | "onClearAllFilters"
>;
export function RebalanceTable({
  baseurl,
  rebalanceList,
  rebalancesCount,
  onApprove,
  onStop,
  filterName,
  filterStatus,
  filterMode,
  onFilterNameChange,
  onFilterStatusChange,
  onFilterModeChange,
  page,
  perPage,
  onPageChange,
  onRefresh,
  onClearAllFilters,
  sortProvider,
}: RebalanceTableProps) {
  const t = useTranslations("Rebalancing");
  return (
    <TableView
      itemCount={rebalancesCount}
      page={page}
      perPage={perPage}
      onPageChange={onPageChange}
      isRowExpandable={() => true}
      columns={RebalanceTableColumns}
      sortProvider={sortProvider}
      data={rebalanceList}
      emptyStateNoData={<EmptyStateNoKafkaRebalance />}
      emptyStateNoResults={
        <EmptyStateNoMatchFound onClear={onClearAllFilters!} />
      }
      onClearAllFilters={onClearAllFilters}
      ariaLabel={"Rebalance brokers"}
      renderHeader={({ column, key }) => {
        const sortAction = sortProvider(column);

        switch (column) {
          case "name":
            return (
              <Th key={key} width={30} dataLabel={"Rebalance"} sort={sortAction}>
                {t("rebalance_name")}
              </Th>
            );
          case "status":
            return (
              <Th key={key} dataLabel={"Status"} sort={sortAction}>
                {t("status")}
              </Th>
            );
          case "lastUpdated":
            return (
              <Th key={key} dataLabel={"Last updated"} sort={sortAction}>
                {t("last_updated")}
              </Th>
            );
        }
      }}
      renderCell={({ Td, column, row, key }) => {
        switch (column) {
          case "name":
            return (
              <Td key={key} dataLabel={"Rebalance Name"} width={10}>
                <Link href={`${baseurl}/rebalances/${row.id}`}>
                  <Badge key={key}>{t("cr_badge")}</Badge> {row.attributes.name}
                </Link>
              </Td>
            );
          case "status":
            return (
              <Td key={key} dataLabel={"Status"}>
                {statusLabel(row.attributes.status).label}
              </Td>
            );
          case "lastUpdated":
            return (
              <Td key={key} dataLabel={"Last updated"}>
                <DateTime
                  value={
                    row.attributes.conditions
                      ?.filter((c) => c.type == row.attributes.status)
                      .map((c) => c.lastTransitionTime!)
                      .find(() => true) ?? row.attributes.creationTimestamp
                  }
                />
              </Td>
            );
        }
      }}
      renderActions={({ row, ActionsColumn }) => (
        <ActionsColumn
          isDisabled={!hasPrivilege("UPDATE", row)}
          items={[
            {
              title: t("approve"),
              onClick: () => onApprove(row),
              isDisabled: !row.meta?.allowedActions.includes("approve"),
            },
            {
              title: t("refresh"),
              onClick: () => onRefresh(row),
              isDisabled: !row.meta?.allowedActions.includes("refresh"),
            },
            {
              title: t("stop"),
              onClick: () => onStop(row),
              isDisabled: !row.meta?.allowedActions.includes("stop"),
            },
          ]}
        />
      )}
      isFiltered={
        filterName !== undefined ||
        filterStatus?.length !== 0 ||
        filterMode?.length !== 0
      }
      getExpandedRow={({ row }) => {
        return (
          <DescriptionList className="pf-v6-u-mt-md pf-v6-u-mb-lg">
            <Flex justifyContent={{ default: "justifyContentSpaceEvenly" }}>
              <FlexItem
                style={{
                  width: "25%",
                }}
              >
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t("auto_approval_enabled")}
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {row.meta.autoApproval === true ? "true" : "false"}
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </FlexItem>
              <FlexItem style={{ width: "50%", paddingRight: "5rem" }}>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t("mode")}
                    {"  "}
                    <Popover
                      aria-label={t("mode")}
                      headerContent={<div>{t("rebalance_mode")}</div>}
                      bodyContent={
                        <div>
                          <List>
                            <ListItem>
                              {
                                <RichText>
                                  {(tags) => t.rich("full_mode", tags)}
                                </RichText>
                              }
                              &nbsp;
                              {t("full_mode_description")}
                            </ListItem>
                            <ListItem>
                              {
                                <RichText>
                                  {(tags) => t.rich("add_brokers_mode", tags)}
                                </RichText>
                              }
                              &nbsp;
                              {t("add_brokers_mode_description")}
                            </ListItem>
                            <ListItem>
                              {
                                <RichText>
                                  {(tags) =>
                                    t.rich("remove_brokers_mode", tags)
                                  }
                                </RichText>
                              }
                              &nbsp;
                              {t("remove_brokers_mode_description")}
                            </ListItem>
                          </List>
                        </div>
                      }
                    >
                      <HelpIcon />
                    </Popover>
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {row.attributes.mode === "full" ? (
                      ModeLabel[row.attributes.mode].label
                    ) : (
                      <>
                        {ModeLabel[row.attributes.mode].label}{" "}
                        {row.attributes.brokers?.length
                          ? row.attributes.brokers.map((b, index) => (
                              <>
                                <Link href={`${baseurl}/${b}`}>
                                  {t("broker", { b })}
                                </Link>
                                {index <
                                  (row.attributes.brokers?.length || 0) - 1 &&
                                  ", "}
                              </>
                            ))
                          : ""}
                      </>
                    )}
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </FlexItem>
            </Flex>
          </DescriptionList>
        );
      }}
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
          validate: (value) => true,
          errorMessage: "At least 3 characters",
        },
        Status: {
          type: "checkbox",
          chips: filterStatus || [],
          onToggle: (status) => {
            const newStatus = filterStatus?.includes(status)
              ? filterStatus.filter((s) => s !== status)
              : [...filterStatus!, status];
            onFilterStatusChange(newStatus);
          },
          onRemoveChip: (status) => {
            const newStatus = (filterStatus || []).filter((s) => s !== status);
            onFilterStatusChange(newStatus);
          },
          onRemoveGroup: () => {
            onFilterStatusChange(undefined);
          },
          options: StatusLabel,
        },
        Mode: {
          type: "checkbox",
          chips: filterMode || [],
          onToggle: (mode) => {
            const newMode = filterMode?.includes(mode)
              ? filterMode.filter((m) => m !== mode)
              : [...filterMode!, mode];
            onFilterModeChange(newMode);
          },
          onRemoveChip: (mode) => {
            const newMode = (filterMode || []).filter((m) => m !== mode);
            onFilterModeChange(newMode);
          },
          onRemoveGroup: () => {
            onFilterModeChange(undefined);
          },
          options: ModeLabel,
        },
      }}
    />
  );
}
