import { AdvancedSearch } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/AdvancedSearch";
import { MessageBrowserProps } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/MessagesTable";
import {
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { EllipsisVIcon } from "@/libs/patternfly/react-icons";
import { useState } from "react";

export function MessagesTableToolbar({
  filterQuery,
  filterWhere,
  filterEpoch,
  filterTimestamp,
  filterOffset,
  filterPartition,
  partitions,
  filterLimit,
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
  | "partitions"
  | "filterLimit"
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
            partitions={partitions}
            onSearch={onSearch}
          />
        </ToolbarItem>

        <ToolbarItem>
          <Dropdown
            popperProps={{ position: "right" }}
            isOpen={isMenuOpen}
            onOpenChange={() => {
              setIsMenuOpen((v) => !v);
            }}
            toggle={(toggleRef) => (
              <MenuToggle
                ref={toggleRef}
                isExpanded={isMenuOpen}
                onClick={() => {
                  setIsMenuOpen(true);
                }}
                variant="plain"
                aria-label="Table actions"
              >
                <EllipsisVIcon aria-hidden="true" />
              </MenuToggle>
            )}
          >
            <DropdownList>
              <DropdownItem onClick={onColumnManagement}>
                Manage columns
              </DropdownItem>
            </DropdownList>
          </Dropdown>
        </ToolbarItem>
      </ToolbarContent>
    </Toolbar>
  );
}
