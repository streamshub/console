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

  function onClearAllFilters() {}

  // const handleQueryChange = useDebouncedCallback((value: string) => {
  //   onQueryChange(value);
  // }, 300);

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
            isOpen={false}
            onOpenChange={(isOpen: boolean) => {}}
            toggle={(toggleRef) => (
              <MenuToggle
                ref={toggleRef}
                isExpanded={false}
                onClick={() => {}}
                variant="plain"
                aria-label="Table options"
              >
                <EllipsisVIcon aria-hidden="true" />
              </MenuToggle>
            )}
          >
            <DropdownList>
              <DropdownItem>Action</DropdownItem>
              <DropdownItem
                // Prevent the default onClick functionality for example purposes
                onClick={() => onColumnManagement()}
              >
                Manage columns
              </DropdownItem>
            </DropdownList>
          </Dropdown>
        </ToolbarItem>
      </ToolbarContent>
    </Toolbar>
  );
}
