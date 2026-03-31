"use client";

import {
  ConsumerGroup,
  GroupType,
  ConsumerGroupState,
} from "@/api/groups/schema";
import { useRouter } from "@/i18n/routing";
import { useFilterParams } from "@/utils/useFilterParams";
import { useEffect, useOptimistic, useState, useTransition } from "react";
import {
  ConsumerGroupColumn,
  ConsumerGroupColumns,
  ConsumerGroupsTable,
  SortableColumns,
} from "./ConsumerGroupsTable";
import { ResetOffsetModal } from "./[groupId]/members/ResetOffsetModal";
import {
  Alert,
  AlertActionCloseButton,
  AlertActionLink,
  Grid,
  GridItem,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { clientConfig as config } from "@/utils/config";
import { ThSortType } from "@/libs/patternfly/react-table";

export type ConnectedConsumerGroupTableProps = {
  kafkaId: string;
  groups: ConsumerGroup[] | undefined;
  consumerGroupCount: number;
  page: number;
  perPage: number;
  id: string | undefined;
  type: GroupType[] | undefined;
  sort: ConsumerGroupColumn;
  sortDir: "asc" | "desc";
  consumerGroupState: ConsumerGroupState[] | undefined;
  baseurl: string;
  nextPageCursor: string | null | undefined;
  prevPageCursor: string | null | undefined;
};

type State = {
  id: string | undefined;
  type: GroupType[] | undefined;
  groups: ConsumerGroup[] | undefined;
  perPage: number;
  sort: ConsumerGroupColumn;
  sortDir: "asc" | "desc";
  consumerGroupState: ConsumerGroupState[] | undefined;
};

export function ConnectedConsumerGroupTable({
  groups,
  consumerGroupCount,
  page,
  perPage,
  id,
  sort,
  sortDir,
  type,
  consumerGroupState,
  baseurl,
  nextPageCursor,
  prevPageCursor,
  kafkaId,
}: ConnectedConsumerGroupTableProps) {
  const router = useRouter();
  const t = useTranslations("GroupsTable");
  const _updateUrl = useFilterParams({ perPage, sort, sortDir });
  const [_, startTransition] = useTransition();
  const [state, addOptimistic] = useOptimistic<
    State,
    Partial<Omit<State, "ConsumerGroups">>
  >(
    {
      groups,
      id,
      type,
      perPage,
      sort,
      sortDir,
      consumerGroupState,
    },
    (state, options) => ({ ...state, ...options, group: undefined }),
  );

  const updateUrl: typeof _updateUrl = (newParams) => {
    const { groups, ...s } = state;
    _updateUrl({
      ...s,
      ...newParams,
    });
  };

  function clearFilters() {
    startTransition(() => {
      _updateUrl({});
      addOptimistic({
        id: undefined,
        type: undefined,
        consumerGroupState: undefined,
      });
    });
  }

  const [isResetOffsetModalOpen, setResetOffsetModalOpen] = useState(false);
  const [consumerGroupMembers, setConsumerGroupMembers] = useState<string[]>(
    [],
  );
  const [consumerGroupId, setConsumerGroupId] = useState<string>("");
  const [isAlertVisible, setAlertVisible] = useState(true);

  const [showLearning, setShowLearning] = useState(false);

  useEffect(() => {
    config().then((cfg) => {
      setShowLearning(cfg.showLearning);
    });
  }, []);

  const handleClose = () => {
    setAlertVisible(false);
  };

  const closeResetOffsetModal = () => {
    setResetOffsetModalOpen(false);
    setConsumerGroupMembers([]);
    router.push(`${baseurl}`);
  };

  const sortProvider = (col: ConsumerGroupColumn): ThSortType | undefined => {
    if (!SortableColumns.includes(col)) {
      return undefined;
    }
    const activeIndex = ConsumerGroupColumns.indexOf(state.sort);
    const columnIndex = ConsumerGroupColumns.indexOf(col);
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
            title={t("groups_alert")}
            actionClose={<AlertActionCloseButton onClose={handleClose} />}
            actionLinks={
              showLearning ? (
                <AlertActionLink component="a" href={t("external_link")}>
                  {t("learn_more")}
                </AlertActionLink>
              ) : null
            }
          />
        )}
      </GridItem>
      <GridItem>
        <ConsumerGroupsTable
          total={consumerGroupCount}
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
          groups={state.groups}
          sortProvider={sortProvider}
          filterName={state.id}
          onFilterNameChange={(id) => {
            startTransition(() => {
              updateUrl({ id });
              addOptimistic({ id });
            });
          }}
          filterType={state.type}
          onFilterTypeChange={(type) => {
            startTransition(() => {
              updateUrl({ type });
              addOptimistic({ type });
            });
          }}
          filterState={state.consumerGroupState}
          onFilterStateChange={(consumerGroupState) => {
            startTransition(() => {
              updateUrl({ consumerGroupState });
              addOptimistic({ consumerGroupState });
            });
          }}
          onClearAllFilters={clearFilters}
          kafkaId={kafkaId}
          onResetOffset={(row) => {
            startTransition(() => {
              if (row.attributes.state === "STABLE") {
                setResetOffsetModalOpen(true);
                setConsumerGroupMembers(
                  row.attributes.members?.map((member) => member.memberId) ||
                    [],
                );
                setConsumerGroupId(row.id);
              } else if (row.attributes.state === "EMPTY") {
                router.push(`${baseurl}/${row.id}/reset-offset`);
              }
            });
          }}
        />
        {isResetOffsetModalOpen && (
          <ResetOffsetModal
            members={consumerGroupMembers}
            isResetOffsetModalOpen={isResetOffsetModalOpen}
            onClickClose={closeResetOffsetModal}
            consumerGroupId={consumerGroupId}
            kafkaId={kafkaId}
          />
        )}
      </GridItem>
    </Grid>
  );
}
