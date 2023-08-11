"use client";
import {
  SearchInput,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { ToolbarGroup } from "@patternfly/react-core";
import { useTranslations } from "next-intl";

export function Search() {
  const t = useTranslations();

  return (
    <Toolbar
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
