"use client";
import { useAppLayout } from "@/app/[locale]/AppLayoutProvider";
import { PageSidebar, PageSidebarBody } from "@/libs/patternfly/react-core";
import { PropsWithChildren } from "react";

export function AppSidebar({ children }: PropsWithChildren) {
  const { sidebarExpanded } = useAppLayout();
  return (
    <PageSidebar isSidebarOpen={sidebarExpanded}>
      <PageSidebarBody>{children}</PageSidebarBody>
    </PageSidebar>
  );
}
