"use client";
import {
  PageSection,
  SearchInput,
  TextContent,
  Title,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { useSelectedLayoutSegment } from "next/navigation";

export function Header() {
  const t = useTranslations();
  const segment = useSelectedLayoutSegment();

  return (
    <PageSection variant={"light"} padding={{ default: "noPadding" }}>
      <TextContent></TextContent>
      <Toolbar
        id="header-toolbar"
        ouiaId={"header-toolbar"}
        clearAllFilters={() => {}}
      >
        <ToolbarContent>
          <ToolbarItem variant={"search-filter"}>
            <SearchInput
              value={segment || ""}
              onChange={(_event, value) => {}}
              onClear={() => {}}
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
    </PageSection>
  );
}
