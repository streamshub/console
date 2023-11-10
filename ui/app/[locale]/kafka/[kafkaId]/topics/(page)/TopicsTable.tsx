"use client";
import { TopicList } from "@/api/topics/schema";
import { ButtonLink } from "@/components/ButtonLink";
import { Bytes } from "@/components/Bytes";
import { Number } from "@/components/Number";
import { TableView } from "@/components/table";
import { useRouter } from "@/navigation";
import { useFilterParams } from "@/utils/useFilterParams";
import { TableVariant } from "@patternfly/react-table";
import { useFormatter, useTranslations } from "next-intl";
import { useTransition } from "react";

export const TopicsTableColumns = [
  "name",
  "messages",
  "consumerGroups",
  "partitions",
  "storage",
] as const;
export type SortableTopicsTableColumns = Exclude<
  TopicsTableColumn,
  "consumerGroups" | "partitions"
>;
export type TopicsTableColumn = (typeof TopicsTableColumns)[number];
export const SortableColumns = ["name", "messages", "storage"];

export type TopicsTableProps = {
  topics: TopicList[] | undefined;
  topicsCount: number;
  canCreate: boolean;
  page: number;
  perPage: number;
  sort: TopicsTableColumn;
  sortDir: "asc" | "desc";
  baseurl: string;
  nextPageCursor: string | null | undefined;
  prevPageCursor: string | null | undefined;
};

export function TopicsTable({
  canCreate,
  topics,
  topicsCount,
  page,
  perPage,
  sort,
  sortDir,
  baseurl,
  nextPageCursor,
  prevPageCursor,
}: TopicsTableProps) {
  const format = useFormatter();
  const t = useTranslations("topics");
  const router = useRouter();
  const updateUrl = useFilterParams({ perPage, sort, sortDir });
  const [_, startTransition] = useTransition();

  return (
    <TableView
      itemCount={topicsCount}
      page={page}
      perPage={perPage}
      onPageChange={(newPage, perPage) => {
        let pageDiff = newPage - page;
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
      }}
      data={topics}
      emptyStateNoData={<div>no data</div>}
      emptyStateNoResults={<div>no results</div>}
      ariaLabel={"Topics"}
      columns={TopicsTableColumns}
      isColumnSortable={(col) => {
        if (!SortableColumns.includes(col)) {
          return undefined;
        }
        const activeIndex = TopicsTableColumns.indexOf(sort);
        const columnIndex = TopicsTableColumns.indexOf(col);
        return {
          label: col as string,
          columnIndex,
          onSort: () => {
            updateUrl({
              sort: col,
              sortDir:
                activeIndex === columnIndex
                  ? sortDir === "asc"
                    ? "desc"
                    : "asc"
                  : "asc",
            });
          },
          sortBy: {
            index: activeIndex,
            direction: sortDir,
            defaultDirection: "asc",
          },
          isFavorites: undefined,
        };
      }}
      // onRowClick={({ row }) => {
      //   startTransition(() => {
      //     router.push(`./topics/${row.id}`);
      //   });
      // }}
      renderHeader={({ Th, column, key }) => {
        switch (column) {
          case "name":
            return (
              <Th key={key} width={40} dataLabel={"Topic"}>
                Name
              </Th>
            );
          case "consumerGroups":
            return (
              <Th key={key} dataLabel={"Consumer groups"}>
                Consumer groups
              </Th>
            );
          case "partitions":
            return (
              <Th key={key} dataLabel={"Partitions"}>
                Partitions
              </Th>
            );
          case "messages":
            return (
              <Th key={key} dataLabel={"Messages"}>
                Messages
              </Th>
            );
          case "storage":
            return (
              <Th key={key} dataLabel={"Storage"}>
                Storage
              </Th>
            );
        }
      }}
      renderCell={({ Td, column, row, key }) => {
        switch (column) {
          case "name":
            return (
              <Td key={key} dataLabel={"Topic"}>
                {row.attributes.name}
              </Td>
            );
          case "consumerGroups":
            return (
              <Td key={key} dataLabel={"Consumer groups"}>
                <ButtonLink
                  variant={"link"}
                  href={`${baseurl}/${row.id}/consumer-groups`}
                >
                  <Number
                    value={row.relationships.consumerGroups.data.length}
                  />
                </ButtonLink>
              </Td>
            );
          case "partitions":
            return (
              <Td key={key} dataLabel={"Partitions"}>
                <ButtonLink
                  variant={"link"}
                  href={`${baseurl}/${row.id}/partitions`}
                >
                  {format.number(row.attributes.partitions.length)}
                </ButtonLink>
              </Td>
            );
          case "messages":
            return (
              <Td key={key} dataLabel={"Messages"}>
                <ButtonLink
                  variant={"link"}
                  href={`${baseurl}/${row.id}/messages`}
                >
                  <Number value={row.attributes.recordCount} />
                </ButtonLink>
              </Td>
            );
          case "storage":
            return (
              <Td key={key} dataLabel={"Storage"}>
                <Bytes value={row.attributes.totalLeaderLogBytes} />
              </Td>
            );
        }
      }}
      renderActions={({ row, ActionsColumn }) => (
        <ActionsColumn
          items={[
            {
              title: "Edit properties",
              onClick: () => {
                startTransition(() => {
                  router.push(`${baseurl}/${row.id}/configuration`);
                });
              },
            },
            {
              isSeparator: true,
            },
            {
              title: "Delete topic",
              onClick: () => {
                startTransition(() => {
                  router.push(`${baseurl}/${row.id}/delete`);
                });
              },
            },
          ]}
        />
      )}
      filters={{
        name: {
          type: "search",
          chips: [],
          onSearch: () => {},
          onRemoveChip: () => {},
          onRemoveGroup: () => {},
          validate: () => true,
          errorMessage: "",
        },
      }}
      actions={
        canCreate
          ? [
              {
                label: t("create_topic"),
                onClick: () => {
                  router.push(baseurl + "/create");
                },
                isPrimary: true,
              },
            ]
          : undefined
      }
      variant={TableVariant.compact}
    />
  );
}
