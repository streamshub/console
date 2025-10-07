import { Nav, Page } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { PropsWithChildren, ReactNode } from "react";
import { AppMasthead } from "./AppMasthead";
import { AppSidebar } from "./AppSidebar";
import { ClusterDrawer } from "./ClusterDrawer";
import { ClusterDrawerProvider } from "./ClusterDrawerProvider";
import { ReconciliationProvider } from "./ReconciliationProvider";
import { ReconciliationPausedBanner } from "./ReconciliationPausedBanner";
import { AlertProvider } from "./AlertProvider";
import { ClusterInfo } from "./AppDropdown";
import { ClusterDetail } from "@/api/kafka/schema";
import { MetadataResponse } from "@/api/meta/schema";

export function AppLayout({
  username,
  sidebar,
  children,
  kafkaDetail,
  metadata,
  clusterInfoList,
  isOidcEnabled,
}: PropsWithChildren<{
  username?: string;
  sidebar?: ReactNode;
  kafkaDetail?: ClusterDetail;
  readonly metadata?: MetadataResponse;
  clusterInfoList?: ClusterInfo[];
  isOidcEnabled?: boolean;
}>) {
  const t = useTranslations();

  const kafkaId = kafkaDetail?.id;
  const isValidKafkaId = !!kafkaId;

  return (
    <Page
      masthead={
        <AppMasthead
          username={username}
          showSidebarToggle={!!sidebar}
          clusterInfoList={clusterInfoList || []}
          kafkaId={kafkaId || ""}
          metadata={metadata}
          isOidcEnabled={isOidcEnabled}
        />
      }
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
        <AlertProvider>
          <ReconciliationProvider kafkaId={kafkaId ?? ""}>
            {isValidKafkaId && (
              <ReconciliationPausedBanner kafkaDetail={kafkaDetail} />
            )}
            <ClusterDrawer>{children}</ClusterDrawer>
          </ReconciliationProvider>
        </AlertProvider>
      </ClusterDrawerProvider>
      {/*</HelpContainer>*/}
    </Page>
  );
}
