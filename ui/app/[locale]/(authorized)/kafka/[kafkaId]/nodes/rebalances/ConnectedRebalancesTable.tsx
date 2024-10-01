"use client";
import { useFilterParams } from "@/utils/useFilterParams";
import { useOptimistic, useState, useTransition } from "react";
import {
  RebalanceTable,
  RebalanceTableColumn,
  RebalanceTableColumns,
} from "./RebalanceTable";
import {
  Alert,
  AlertActionCloseButton,
  AlertActionLink,
  Grid,
  GridItem,
  PageSection,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { RebalancesCountCard } from "./RebalancesCountCard";
import { RebalanceList, RebalanceStatus } from "@/api/rebalance/schema";
import { ValidationModal } from "./ValidationModal";
import { getRebalanceDetails } from "@/api/rebalance/actions";
import { EmptyStateNoKafkaRebalance } from "./EmptyStateNoKafkaRebalance";

export type ConnectedReabalancesTableProps = {
  rebalances: RebalanceList[] | undefined;
  rebalancesCount: number;
  page: number;
  perPage: number;
  mode: string | undefined;
  name: string | undefined;
  sort: RebalanceTableColumn;
  sortDir: "asc" | "desc";
  status: RebalanceStatus[] | undefined;
  baseurl: string;
  nextPageCursor: string | null | undefined;
  prevPageCursor: string | null | undefined;
  kafkaId: string;
};

type State = {
  name: string | undefined;
  rebalances: RebalanceList[] | undefined;
  perPage: number;
  sort: RebalanceTableColumn;
  sortDir: "asc" | "desc";
  status: RebalanceStatus[] | undefined;
  mode: string | undefined;
};

export function ConnectedReabalancesTable({
  rebalances,
  rebalancesCount,
  page,
  perPage,
  name,
  sort,
  sortDir,
  status,
  nextPageCursor,
  prevPageCursor,
  mode,
  baseurl,
  kafkaId,
}: ConnectedReabalancesTableProps) {
  const t = useTranslations("Rebalancing");

  const [isAlertVisible, setAlertVisible] = useState(true);

  const [isModalOpen, setModalOpen] = useState(false);

  const [approvalStatus, setApprovalStatus] = useState<"approve" | "stop">("approve");

  const [RebalanceId, setRebalanceId] = useState<string>("");

  const handleClose = () => {
    setAlertVisible(false);
  };

  const _updateUrl = useFilterParams({ perPage, sort, sortDir });
  const [_, startTransition] = useTransition();
  const [state, addOptimistic] = useOptimistic<
    State,
    Partial<Omit<State, "kafkaRebalances">>
  >(
    {
      rebalances,
      name,
      perPage,
      sort,
      sortDir,
      status,
      mode,
    },
    (state, options) => ({ ...state, ...options, rebalances: undefined }),
  );

  const updateUrl: typeof _updateUrl = (newParams) => {
    const { rebalances, ...s } = state;
    _updateUrl({
      ...s,
      ...newParams,
    });
  };

  function clearFilters() {
    startTransition(() => {
      _updateUrl({});
      addOptimistic({
        name: undefined,
        mode: undefined,
        status: undefined,
      });
    });
  }

  const statusCounts = rebalances?.reduce(
    (acc, rebalance) => {
      const status = rebalance.attributes.status;
      if (status === "ProposalReady") acc.proposalReady += 1;
      if (status === "Rebalancing") acc.rebalancing += 1;
      if (status === "Ready") acc.ready += 1;
      if (status === "Stopped") acc.stopped += 1;
      return acc;
    },
    { proposalReady: 0, rebalancing: 0, ready: 0, stopped: 0 },
  ) || { proposalReady: 0, rebalancing: 0, ready: 0, stopped: 0 };

  const onConfirm = async () => {
    await getRebalanceDetails(kafkaId, RebalanceId, approvalStatus);
    setModalOpen(false);
  };

  return (
    <PageSection hasOverflowScroll={true} aria-label={"rebalancing"}>
      {rebalances?.length === 0 ? (
        <EmptyStateNoKafkaRebalance />
      ) : (
        <Grid hasGutter>
          <GridItem>
            {isAlertVisible && (
              <Alert
                variant="info"
                isInline
                title={t("cruisecontrol_enable")}
                actionClose={<AlertActionCloseButton onClose={handleClose} />}
                actionLinks={
                  <AlertActionLink component="a" href="#">
                    {t("learn_more_about_cruisecontrol_enablement")}
                  </AlertActionLink>
                }
              />
            )}
          </GridItem>
          <GridItem>
            <RebalancesCountCard
              TotalRebalancing={rebalancesCount}
              proposalReady={statusCounts.proposalReady}
              rebalancing={statusCounts.rebalancing}
              ready={statusCounts.ready}
              stopped={statusCounts.stopped}
            />
          </GridItem>
          <GridItem>
            <RebalanceTable
              page={page}
              perPage={state.perPage}
              onPageChange={(newPage, perPage) => {
                startTransition(() => {
                  const pageDiff = newPage - page;
                  switch (pageDiff) {
                    case -1:
                      updateUrl({ perPage, page: prevPageCursor });
                      break;
                    case 1:
                      updateUrl({ perPage, page: nextPageCursor });
                      break;
                    default:
                      updateUrl({ perPage });
                      break;
                  }
                  addOptimistic({ perPage });
                });
              }}
              rebalanceList={state.rebalances}
              rebalancesCount={rebalancesCount}
              isColumnSortable={(col) => {
                if (!RebalanceTableColumns.includes(col)) {
                  return undefined;
                }
                const activeIndex = RebalanceTableColumns.indexOf(state.sort);
                const columnIndex = RebalanceTableColumns.indexOf(col);
                return {
                  label: col as string,
                  columnIndex,
                  onSort: () => {
                    startTransition(() => {
                      const newSortDir =
                        activeIndex === columnIndex
                          ? state.sortDir === "asc"
                            ? "desc"
                            : "asc"
                          : "asc";
                      updateUrl({
                        sort: col,
                        sortDir: newSortDir,
                      });
                      addOptimistic({ sort: col, sortDir: newSortDir });
                    });
                  },
                  sortBy: {
                    index: activeIndex,
                    direction: state.sortDir,
                    defaultDirection: "asc",
                  },
                  isFavorites: undefined,
                };
              }}
              filterName={state.name}
              onFilterNameChange={(name) => {
                startTransition(() => {
                  updateUrl({ name });
                  addOptimistic({ name });
                });
              }}
              filterMode={state.mode}
              onFilterModeChange={(mode) => {
                startTransition(() => {
                  updateUrl({ mode });
                  addOptimistic({ mode });
                });
              }}
              filterStatus={state.status}
              onFilterStatusChange={(status) => {
                startTransition(() => {
                  updateUrl({ status });
                  addOptimistic({ status });
                });
              }}
              onClearAllFilters={clearFilters}
              onApprove={(row) => {
                setModalOpen(true);
                setApprovalStatus("approve");
                setRebalanceId(row.id);
              }}
              onStop={(row) => {
                setModalOpen(true);
                setApprovalStatus("stop");
                setRebalanceId(row.id);
              }}
              baseurl={baseurl}
            />
          </GridItem>
          {isModalOpen && (
            <ValidationModal
              status={approvalStatus}
              isModalOpen={isModalOpen}
              onConfirm={onConfirm}
              onCancel={() => setModalOpen(false)}
            />
          )}
        </Grid>
      )}
    </PageSection>
  );
}
