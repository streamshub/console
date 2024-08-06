import {
  Banner,
  Nav,
  Page,
  Split,
  SplitItem,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { User } from "next-auth";
import { useTranslations } from "next-intl";
import { PropsWithChildren, ReactNode } from "react";
import { AppMasthead } from "./AppMasthead";
import { AppSidebar } from "./AppSidebar";
import { ClusterDrawer } from "./ClusterDrawer";

import { ClusterDrawerProvider } from "./ClusterDrawerProvider";
import { TechPreviewPopover } from "./TechPreviewPopover";

export function AppLayout({
  username,
  sidebar,
  children,
}: PropsWithChildren<{ username?: string; sidebar?: ReactNode }>) {
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
        <ClusterDrawer>
          <Banner variant={"blue"}>
            <Split>
              <SplitItem isFilled={true}>
                {t("AppLayout.tech_preview_label")}
              </SplitItem>
              <SplitItem>
                <TechPreviewPopover>
                  <HelpIcon />
                </TechPreviewPopover>
              </SplitItem>
            </Split>
          </Banner>
          {children}
        </ClusterDrawer>
      </ClusterDrawerProvider>
      {/*</HelpContainer>*/}
    </Page>
  );
}
