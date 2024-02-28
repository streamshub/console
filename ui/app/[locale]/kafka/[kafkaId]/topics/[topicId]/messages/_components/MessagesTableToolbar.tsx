import { AdvancedSearch } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/AdvancedSearch";
import { MessageBrowserProps } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/MessagesTable";
import {
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { Button, Tooltip } from "@patternfly/react-core";
import { ColumnsIcon } from "@patternfly/react-icons";
import { useState } from "react";

export function MessagesTableToolbar({
  filterQuery,
  filterWhere,
  filterEpoch,
  filterTimestamp,
  filterOffset,
  filterPartition,
  filterLimit,
  filterLive,
  partitions,
  onSearch,
  onColumnManagement,
}: Pick<
  MessageBrowserProps,
  | "filterQuery"
  | "filterWhere"
  | "filterEpoch"
  | "filterTimestamp"
  | "filterOffset"
  | "filterPartition"
  | "filterLimit"
  | "filterLive"
  | "partitions"
  | "onSearch"
> & {
  onColumnManagement: () => void;
}) {
  const toolbarBreakpoint = "md";
  const [isMenuOpen, setIsMenuOpen] = useState(false);

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
            filterLive={filterLive}
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
