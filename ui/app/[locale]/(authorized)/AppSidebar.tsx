"use client";
import { PageSidebar, PageSidebarBody } from "@/libs/patternfly/react-core";
import { PropsWithChildren } from "react";
import { useAppLayout } from "./AppLayoutProvider";

export function AppSidebar({ children }: PropsWithChildren) {
  const { sidebarExpanded } = useAppLayout();
  return (
    <PageSidebar isSidebarOpen={sidebarExpanded}>
      <PageSidebarBody>{children}</PageSidebarBody>
    </PageSidebar>
  );
}
