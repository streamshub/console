import {
  Banner,
  Bullseye,
  Button,
  Flex,
  FlexItem,
  Nav,
  Page,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { PropsWithChildren, ReactNode } from "react";
import { AppMasthead } from "./AppMasthead";
import { AppSidebar } from "./AppSidebar";
import { ClusterDrawer } from "./ClusterDrawer";

import { ClusterDrawerProvider } from "./ClusterDrawerProvider";

export function AppLayout({
  username,
  sidebar,
  children,
  reconciliationPaused,
}: PropsWithChildren<{
  username?: string;
  sidebar?: ReactNode;
  reconciliationPaused?: boolean;
}>) {
  console.log("reconciliationPaused", reconciliationPaused);
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
          {reconciliationPaused && (
            <Banner variant="gold">
              <Bullseye>
                <Flex>
                  <FlexItem spacer={{ default: "spacerNone" }}>
                    {t("reconciliation.reconciliation_paused_warning")}
                  </FlexItem>
                  <FlexItem spacer={{ default: "spacerLg" }}>
                    <Button variant="link" isInline>
                      {t("reconciliation.resume")}
                    </Button>
                  </FlexItem>
                </Flex>
              </Bullseye>
            </Banner>
          )}
          {children}
        </ClusterDrawer>
      </ClusterDrawerProvider>
      {/*</HelpContainer>*/}
    </Page>
  );
}
