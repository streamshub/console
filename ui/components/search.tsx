"use client";
import {
  SearchInput,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { ToolbarGroup } from "@patternfly/react-core";

export function Search() {
  return (
    <Toolbar
      className={"pf-v5-theme-dark"}
      id="header-toolbar"
      ouiaId={"header-toolbar"}
      clearAllFilters={() => {}}
    >
      <ToolbarContent>
        <ToolbarGroup isOverflowContainer={true} variant={"filter-group"}>
          <ToolbarItem isOverflowContainer={true} variant={"search-filter"}>
            <SearchInput
              className={"pf-v5-u-stretch"}
              value={""}
              onChange={(_event, value) => {}}
              onClear={() => {}}
            />
          </ToolbarItem>
        </ToolbarGroup>
      </ToolbarContent>
    </Toolbar>
  );
}
