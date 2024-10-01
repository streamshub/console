import { TableView, TableViewProps } from "@/components/Table";
import { useTranslations } from "next-intl";
import { RebalanceList, RebalanceStatus } from "@/api/rebalance/schema";
import {
  Flex,
  FlexItem,
  Icon,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Badge,
} from "@/libs/patternfly/react-core";
import {
  CheckIcon,
  ExclamationCircleIcon,
  PauseCircleIcon,
  PendingIcon,
  ResourcesFullIcon,
} from "@/libs/patternfly/react-icons";
import Link from "next/link";
import React, { ReactNode } from "react";

export const RebalanceTableColumns = ["name", "status", "createdAt"] as const;

export type RebalanceTableColumn = (typeof RebalanceTableColumns)[number];

const StatusLabel: Record<RebalanceStatus, ReactNode> = {
  New: (
    <>
      <Icon>
        <ExclamationCircleIcon />
      </Icon>
      &nbsp; New
    </>
  ),
  PendingProposal: (
    <>
      <Icon>
        <PendingIcon />
      </Icon>
      &nbsp;PendingProposal
    </>
  ),
  ProposalReady: (
    <>
      <Icon>
        <CheckIcon />
      </Icon>
      &nbsp;ProposalReady
    </>
  ),
  Stopped: (
    <>
      <Icon>
        <ResourcesFullIcon />
      </Icon>
      &nbsp;Stopped
    </>
  ),
  Rebalancing: (
    <>
      <Icon>
        <PendingIcon />
      </Icon>
      &nbsp;Rebalancing
    </>
  ),
  NotReady: (
    <>
      <Icon>
        <PendingIcon />
      </Icon>
      &nbsp;NotReady
    </>
  ),
  Ready: (
    <>
      <Icon>
        <CheckIcon />
      </Icon>
      &nbsp;Ready
    </>
  ),
  ReconciliationPaused: (
    <>
      <Icon>
        <PauseCircleIcon />
      </Icon>
      &nbsp;ReconciliationPaused
    </>
  ),
};

export type RebalanceTabelProps = {
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
  filterMode: string | undefined;
  onFilterNameChange: (name: string | undefined) => void;
  onFilterModeChange: (mode: string | undefined) => void;
  onFilterStatusChange: (status: RebalanceStatus[] | undefined) => void;
} & Pick<
  TableViewProps<RebalanceList, (typeof RebalanceTableColumns)[number]>,
  "isColumnSortable" | "onPageChange" | "onClearAllFilters"
>;
export function RebalanceTable({
  baseurl,
  isColumnSortable,
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
}: RebalanceTabelProps) {
  const t = useTranslations("Rebalancing");
  return (
    <TableView
      itemCount={rebalancesCount}
      page={page}
      perPage={perPage}
      onPageChange={onPageChange}
      isRowExpandable={() => true}
      columns={RebalanceTableColumns}
      isColumnSortable={isColumnSortable}
      data={rebalanceList}
      emptyStateNoData={<></>}
      emptyStateNoResults={<></>}
      ariaLabel={"Rebalance brokers"}
      renderHeader={({ Th, column, key }) => {
        switch (column) {
          case "name":
            return (
              <Th key={key} width={30} dataLabel={"Rebalance"}>
                {t("rebalance_name")}
              </Th>
            );
          case "status":
            return (
              <Th key={key} dataLabel={"Status"}>
                {t("status")}
              </Th>
            );
          case "createdAt":
            return (
              <Th key={key} dataLabel={"Consumer groups"}>
                {t("created_at")}
              </Th>
            );
        }
      }}
      renderCell={({ Td, column, row, key }) => {
        switch (column) {
          case "name":
            return (
              <Td key={key} dataLabel={"Rebalance Name"} width={10}>
                <Link href={`${baseurl}/${row.id}`}>
                  <Badge key={key}>{t("cr_badge")}</Badge> {row.attributes.name}
                </Link>
              </Td>
            );
          case "status":
            return (
              <Td key={key} dataLabel={"Status"}>
                {StatusLabel[row.attributes.status]}
              </Td>
            );
          case "createdAt":
            return (
              <Td key={key} dataLabel={"Created At"}>
                {row.attributes.creationTimestamp}
              </Td>
            );
        }
      }}
      renderActions={({ row, ActionsColumn }) => (
        <ActionsColumn
          items={[
            {
              title: t("approve"),
              onClick: () => onApprove(row),
              isDisabled: row.meta?.allowedActions.includes("approve"),
            },
            {
              title: t("refresh"),
              onClick: () => onRefresh(row),
            },
            {
              title: t("stop"),
              onClick: () => onStop(row),
              isDisabled: !row.meta?.allowedActions.includes("approve"),
            },
          ]}
        />
      )}
      getExpandedRow={({ row }) => {
        return (
          <Flex className={"pf-v5-u-pl-xl"}>
            <DescriptionList columnModifier={{ lg: "3Col" }}>
              <FlexItem>
                <DescriptionListGroup>
                  <DescriptionListTerm>{t("mode")}</DescriptionListTerm>
                  <DescriptionListDescription>
                    {row.attributes.mode}
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </FlexItem>
              <FlexItem className={"pf-v5-u-ml-auto-on-xl"}>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t("auto_approval_enabled")}
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {row.meta.autoApproval === true ? "true" : "false"}
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </FlexItem>
              <FlexItem className={"pf-v5-u-ml-auto-on-xl"}>
                <DescriptionListGroup>
                  <DescriptionListTerm>{t("brokers")}</DescriptionListTerm>
                  <DescriptionListDescription>
                    {row.attributes.brokers?.join(",")}
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </FlexItem>
            </DescriptionList>
          </Flex>
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
        mode: {
          type: "search",
          chips: filterMode ? [filterMode] : [],
          onSearch: onFilterModeChange,
          onRemoveChip: () => {
            onFilterModeChange(undefined);
          },
          onRemoveGroup: () => {
            onFilterModeChange(undefined);
          },
          validate: (value) => true,
          errorMessage: "At least 3 characters",
        },
      }}
    />
  );
}
