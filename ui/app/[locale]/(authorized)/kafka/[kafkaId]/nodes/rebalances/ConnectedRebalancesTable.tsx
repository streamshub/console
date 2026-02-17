"use client";
import { useFilterParams } from "@/utils/useFilterParams";
import { useOptimistic, useState, useTransition, useEffect } from "react";
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
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { RebalancesCountCard } from "./RebalancesCountCard";
import {
  RebalanceList,
  RebalanceMode,
  RebalanceStatus,
} from "@/api/rebalance/schema";
import { ValidationModal } from "./ValidationModal";
import { patchRebalance } from "@/api/rebalance/actions";
import { useAlert } from "@/components/AlertContext";
import { clientConfig as config } from "@/utils/config";
import { ThSortType } from "@patternfly/react-table/dist/esm/components/Table/base/types";

export type ConnectedReabalancesTableProps = {
  rebalances: RebalanceList[] | undefined;
  rebalancesCount: number;
  page: number;
  perPage: number;
  mode: RebalanceMode[] | undefined;
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
  mode: RebalanceMode[] | undefined;
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

  const { addAlert } = useAlert();

  const [isAlertVisible, setAlertVisible] = useState(true);

  const [isModalOpen, setModalOpen] = useState(false);

  const [showLearning, setShowLearning] = useState(false);

  useEffect(() => {
    config().then((cfg) => {
      setShowLearning(cfg.showLearning);
    });
  }, []);

  const [approvalStatus, setApprovalStatus] = useState<
    "approve" | "stop" | "refresh"
  >("approve");

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

  const alertMessage =
    approvalStatus === "approve"
      ? t("approved_alert")
      : approvalStatus === "stop"
        ? t("stopped_alert")
        : t("refresh_alert");

  const onConfirm = async () => {
    await patchRebalance(kafkaId, RebalanceId, approvalStatus);
    setModalOpen(false);
    addAlert({
      title: alertMessage,
      variant: "success",
    });
  };

  const sortProvider = (col: RebalanceTableColumn): ThSortType | undefined => {
    if (!RebalanceTableColumns.includes(col)) {
      return undefined;
    }
    const activeIndex = RebalanceTableColumns.indexOf(state.sort);
    const columnIndex = RebalanceTableColumns.indexOf(col);
    return {
      //label: col as string,
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
  };

  return (
    <Grid hasGutter>
      <GridItem>
        {isAlertVisible && (
          <Alert
            variant="info"
            isInline
            title={t("cruisecontrol_enable")}
            actionClose={<AlertActionCloseButton onClose={handleClose} />}
            actionLinks={
              showLearning ? (
                <AlertActionLink component="a" href={t("external_link")}>
                  {t("learn_more_about_cruisecontrol_enablement")}
                </AlertActionLink>
              ) : null
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
          onClearAllFilters={clearFilters}
          rebalanceList={state.rebalances}
          rebalancesCount={rebalancesCount}
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
          onApprove={(row) => {
            setModalOpen(true);
            setApprovalStatus("approve");
            setRebalanceId(row.id);
          }}
          onRefresh={(row) => {
            setModalOpen(true);
            setApprovalStatus("refresh");
            setRebalanceId(row.id);
          }}
          onStop={(row) => {
            setModalOpen(true);
            setApprovalStatus("stop");
            setRebalanceId(row.id);
          }}
          baseurl={baseurl}
          sortProvider={sortProvider}
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
  );
}
