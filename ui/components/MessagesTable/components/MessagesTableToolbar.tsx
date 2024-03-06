import {
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { Button, Tooltip } from "@patternfly/react-core";
import { ColumnsIcon } from "@patternfly/react-icons";
import { MessagesTableProps } from "../MessagesTable";
import { AdvancedSearch } from "./AdvancedSearch";

export function MessagesTableToolbar({
  filterQuery,
  filterWhere,
  filterEpoch,
  filterTimestamp,
  filterOffset,
  filterPartition,
  filterLimit,
  partitions,
  onSearch,
  onColumnManagement,
}: Pick<
  MessagesTableProps,
  | "filterQuery"
  | "filterWhere"
  | "filterEpoch"
  | "filterTimestamp"
  | "filterOffset"
  | "filterPartition"
  | "filterLimit"
  | "partitions"
  | "onSearch"
> & {
  onColumnManagement: () => void;
}) {
  const toolbarBreakpoint = "md";

  function onClearAllFilters() {}

  return (
    <Toolbar
      clearAllFilters={onClearAllFilters}
      collapseListedFiltersBreakpoint={toolbarBreakpoint}
    >
      <ToolbarContent>
        <ToolbarItem
          variant={"search-filter"}
          widths={{ default: "calc(100% - 58px)" }}
        >
          <AdvancedSearch
            filterQuery={filterQuery}
            filterWhere={filterWhere}
            filterEpoch={filterEpoch}
            filterOffset={filterOffset}
            filterPartition={filterPartition}
            filterTimestamp={filterTimestamp}
            filterLimit={filterLimit}
            partitions={partitions}
            onSearch={onSearch}
          />
        </ToolbarItem>

        <ToolbarItem>
          <Tooltip content={"Manage columns"}>
            <Button onClick={onColumnManagement} variant={"plain"}>
              <ColumnsIcon />
            </Button>
          </Tooltip>
        </ToolbarItem>
      </ToolbarContent>
    </Toolbar>
  );
}