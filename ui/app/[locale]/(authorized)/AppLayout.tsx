import { getKafkaClusters } from "@/api/kafka/actions";
import { NavItemLink } from "@/components/Navigation/NavItemLink";
import { TechPreviewPopover } from "@/components/TechPreviewPopover";
import {
  Banner,
  Nav,
  NavGroup,
  NavList,
  Page,
  Split,
  SplitItem,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";
import { PropsWithChildren, Suspense } from "react";
import { AppMasthead } from "./AppMasthead";
import { AppSidebar } from "./AppSidebar";
import { ClusterDrawer } from "./ClusterDrawer";

import { ClusterDrawerProvider } from "./ClusterDrawerProvider";

export function AppLayout({ children }: PropsWithChildren) {
  const t = useTranslations();
  return (
    <Page
      header={<AppMasthead />}
      sidebar={
        <AppSidebar>
          <Nav aria-label={t("AppLayout.main_navigation_aria_label")}>
            <Suspense>
              <ClusterLinks />
            </Suspense>
          </Nav>
        </AppSidebar>
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

async function ClusterLinks() {
  const t = useTranslations();
  const clusters = await getKafkaClusters();
  const defaultCluster = clusters[0];
  if (defaultCluster) {
    return (
      <NavList>
        <NavGroup title={defaultCluster.attributes.name}>
          <NavItemLink url={`/kafka/${defaultCluster.id}/overview`}>
            {t("AppLayout.cluster_overview")}
          </NavItemLink>
          <NavItemLink url={`/kafka/${defaultCluster.id}/topics`}>
            {t("AppLayout.topics")}
          </NavItemLink>
          <NavItemLink url={`/kafka/${defaultCluster.id}/nodes`}>
            {t("AppLayout.brokers")}
          </NavItemLink>
          {/*
      <NavItemLink url={`/kafka/${defaultCluster.id}/service-registry`}>
        Service registry
      </NavItemLink>
*/}
          <NavItemLink url={`/kafka/${defaultCluster.id}/consumer-groups`}>
            {t("AppLayout.consumer_groups")}
          </NavItemLink>
        </NavGroup>
      </NavList>
    );
  }
  return null;
}
