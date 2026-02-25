"use client";
import { TopicsResponse, TopicStatus } from "@/api/topics/schema";
import {
  SortableColumns,
  TopicsTable,
  TopicsTableColumn,
  TopicsTableColumns,
} from "@/components/TopicsTable/TopicsTable";
import { useRouter } from "@/i18n/routing";
import { clientConfig as config } from "@/utils/config";
import { useFilterParams } from "@/utils/useFilterParams";
import { useEffect, useState, useOptimistic, useTransition } from "react";
import { ThSortType } from "@patternfly/react-table/dist/esm/components/Table/base/types";

export type ConnectedTopicsTableProps = {
  topics: TopicsResponse | undefined;
  topicsCount: number;
  page: number;
  perPage: number;
  id: string | undefined;
  name: string | undefined;
  sort: TopicsTableColumn;
  sortDir: "asc" | "desc";
  includeHidden: boolean;
  status: TopicStatus[] | undefined;
  baseurl: string;
  nextPageCursor: string | null | undefined;
  prevPageCursor: string | null | undefined;
};

type State = {
  id: string | undefined;
  name: string | undefined;
  topics: TopicsResponse | undefined;
  perPage: number;
  sort: TopicsTableColumn;
  sortDir: "asc" | "desc";
  includeHidden: boolean;
  status: TopicStatus[] | undefined;
};

export function ConnectedTopicsTable({
  topics,
  topicsCount,
  page,
  perPage,
  id,
  name,
  sort,
  sortDir,
  includeHidden,
  status,
  baseurl,
  nextPageCursor,
  prevPageCursor,
}: ConnectedTopicsTableProps) {
  const router = useRouter();
  const _updateUrl = useFilterParams({ perPage, sort, sortDir, includeHidden });
  const [_, startTransition] = useTransition();
  const [state, addOptimistic] = useOptimistic<
    State,
    Partial<Omit<State, "topics">>
  >(
    {
      topics,
      id,
      name,
      perPage,
      sort,
      sortDir,
      includeHidden,
      status,
    },
    (state, options) => ({ ...state, ...options, topics: undefined }),
  );

  const updateUrl: typeof _updateUrl = (newParams) => {
    const { topics, ...s } = state;
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
        id: undefined,
        status: undefined,
      });
    });
  }

  const [isReadOnly, setIsReadOnly] = useState(true);

  useEffect(() => {
    config().then(cfg => {
      setIsReadOnly(cfg.readOnly);
    })
  }, []);

  const sortProvider = (col: TopicsTableColumn): ThSortType | undefined => {
    if (!SortableColumns.includes(col)) {
      return undefined;
    }
    const activeIndex = TopicsTableColumns.indexOf(state.sort);
    const columnIndex = TopicsTableColumns.indexOf(col);
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
  }

  return (
    <TopicsTable
      baseurl={baseurl}
      topics={state.topics}
      topicsCount={topicsCount}
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
      filterName={state.name}
      onFilterNameChange={(name) => {
        startTransition(() => {
          updateUrl({ name });
          addOptimistic({ name });
        });
      }}
      filterId={state.id}
      onFilterIdChange={(id) => {
        startTransition(() => {
          updateUrl({ id });
          addOptimistic({ id });
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
      onCreateTopic={() => {
        startTransition(() => {
          router.push(baseurl + "/create");
        });
      }}
      onDeleteTopic={(row) => {
        startTransition(() => {
          router.push(`${baseurl}/${row.id}/delete`);
        });
      }}
      onEditTopic={(row) => {
        startTransition(() => {
          router.push(`${baseurl}/${row.id}/configuration`);
        });
      }}
      onInternalTopicsChange={(checked) => {
        startTransition(() => {
          updateUrl({ includeHidden: checked });
          addOptimistic({ includeHidden: checked });
        });
      }}
      includeHidden={includeHidden}
      isReadOnly={isReadOnly}
      sortProvider={sortProvider}
    />
  );
}
