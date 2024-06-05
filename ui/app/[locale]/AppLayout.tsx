import { getKafkaClusters } from "@/api/kafka/actions";
import { AppMasthead } from "@/app/[locale]/AppMasthead";
import { AppSidebar } from "@/app/[locale]/AppSidebar";
import { ClusterDrawer } from "@/app/[locale]/ClusterDrawer";

import { ClusterDrawerProvider } from "@/app/[locale]/ClusterDrawerProvider";
import { NavExpandable } from "@/components/NavExpandable";
import { NavItemLink } from "@/components/Navigation/NavItemLink";
import { TechPreviewPopover } from "@/components/TechPreviewPopover";
import {
  Banner,
  Nav,
  NavList,
  Page,
  Split,
  SplitItem,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";
import { PropsWithChildren, Suspense } from "react";

export function AppLayout({ children }: PropsWithChildren) {
  const t = useTranslations();
  return (
    <Page
      header={<AppMasthead />}
      sidebar={
        <AppSidebar>
          <Nav aria-label={t("AppLayout.main_navigation_aria_label")}>
            <NavList>
              <NavItemLink url={"/home"}>{t("AppLayout.home")}</NavItemLink>
              <NavExpandable
                title={t("AppLayout.kafka_clusters")}
                url={"/kafka"}
                startExpanded={true}
              >
                <Suspense>
                  <Clusters />
                </Suspense>
              </NavExpandable>
            </NavList>
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

async function Clusters() {
  const t = useTranslations();
  const clusters = await getKafkaClusters();
  return clusters
    .filter((c) => c.meta.configured === true)
    .map((s, idx) => (
      <NavExpandable
        key={s.id}
        title={s.attributes.name}
        url={`/kafka/${s.id}`}
        startExpanded={idx === 0}
      >
        <NavItemLink url={`/kafka/${s.id}/overview`}>
          {t("AppLayout.cluster_overview")}
        </NavItemLink>
        <NavItemLink url={`/kafka/${s.id}/topics`}>
          {t("AppLayout.topics")}
        </NavItemLink>
        <NavItemLink url={`/kafka/${s.id}/nodes`}>
          {t("AppLayout.brokers")}
        </NavItemLink>
        {/*
      <NavItemLink url={`/kafka/${s.id}/service-registry`}>
        Service registry
      </NavItemLink>
*/}
        <NavItemLink url={`/kafka/${s.id}/consumer-groups`}>
          {t("AppLayout.consumer_groups")}
        </NavItemLink>
      </NavExpandable>
    ));
}
