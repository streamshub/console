import { usePathname, useSearchParams } from "next/navigation";
import { useRouter } from "next/router";
import type { ResponsiveThProps } from "./ResponsiveTable";

const SortDirectionValues = ["asc", "desc"] as const;
export type SortDirection = (typeof SortDirectionValues)[number];

export function useSortableSearchParams<T extends string>(
  columns: readonly T[],
  labels: { [column in T]: string },
  initialSortColumn: T | null = null,
  initialSortDirection: SortDirection = "asc"
): [
  (
    column: string
  ) => (ResponsiveThProps["sort"] & { label: string }) | undefined,
  T | null,
  SortDirection
] {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const sortParam = searchParams.get("sort") as T | null;
  const directionParam = searchParams.get("dir") as SortDirection | null;

  const activeSortColumn =
    sortParam && columns.includes(sortParam) ? sortParam : initialSortColumn;

  const activeSortDirection =
    directionParam && SortDirectionValues.includes(directionParam)
      ? directionParam
      : initialSortDirection;

  return [
    (column) => {
      const columnIndex = columns.indexOf(column as T);
      return columnIndex >= 0
        ? {
            columnIndex,
            onSort: (_event, index, direction) => {
              if (columns[index]) {
                const params = new URLSearchParams(searchParams);
                params.set("sort", columns[index]);
                params.set("dir", direction);
                void router.push(pathname + "?" + params.toString());
              }
            },
            sortBy: {
              index: activeSortColumn
                ? columns.indexOf(activeSortColumn)
                : undefined,
              direction: activeSortDirection ? activeSortDirection : undefined,
              defaultDirection: initialSortDirection,
            },
            label: labels[column as T],
          }
        : undefined;
    },
    activeSortColumn ? activeSortColumn : null,
    activeSortDirection,
  ];
}
