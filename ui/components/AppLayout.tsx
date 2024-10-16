import { Nav, Page } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { PropsWithChildren, ReactNode } from "react";
import { AppMasthead } from "./AppMasthead";
import { AppSidebar } from "./AppSidebar";
import { ClusterDrawer } from "./ClusterDrawer";

import { ClusterDrawerProvider } from "./ClusterDrawerProvider";
import { ReconciliationProvider } from "./ReconciliationProvider";
import { ReconciliationPausedBanner } from "./ReconciliationPausedBanner";

export function AppLayout({
  username,
  sidebar,
  children,
  kafkaId,
}: PropsWithChildren<{
  username?: string;
  sidebar?: ReactNode;
  kafkaId?: string;
}>) {
  const t = useTranslations();

  return (
    <Page
      header={<AppMasthead username={username} showSidebarToggle={!!sidebar} />}
      sidebar={
        sidebar && (
          <AppSidebar>
            <Nav aria-label={t("AppLayout.main_navigation_aria_label")}>
              {sidebar}
            </Nav>
          </AppSidebar>
        )
      }
    >
      {/*<HelpContainer>*/}
      <ClusterDrawerProvider>
        <ReconciliationProvider>
          <ReconciliationPausedBanner kafkaId={kafkaId} />
          <ClusterDrawer>{children}</ClusterDrawer>
        </ReconciliationProvider>
      </ClusterDrawerProvider>
      {/*</HelpContainer>*/}
    </Page>
  );
}
