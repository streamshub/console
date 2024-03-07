import { getKafkaClusters } from "@/api/kafka/actions";
import { AppMasthead } from "@/app/[locale]/AppMasthead";
import { AppSidebar } from "@/app/[locale]/AppSidebar";
import { ClusterDrawer } from "@/app/[locale]/ClusterDrawer";

import { ClusterDrawerProvider } from "@/app/[locale]/ClusterDrawerProvider";
import { NavExpandable } from "@/components/NavExpandable";
import { NavItemLink } from "@/components/NavItemLink";
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
import { PropsWithChildren, Suspense } from "react";

export function AppLayout({ children }: PropsWithChildren) {
  return (
    <Page
      header={<AppMasthead />}
      sidebar={
        <AppSidebar>
          <Nav aria-label="Nav">
            <NavList>
              <NavItemLink url={"/home"}>Home</NavItemLink>
              <NavExpandable
                title={"Kafka clusters"}
                url={"/kafka"}
                startExpanded={true}
              >
                <Suspense>
                  <Clusters />
                </Suspense>
              </NavExpandable>
              {/*<NavItemLink url={"/learning-resources"}>*/}
              {/*  Learning resources*/}
              {/*</NavItemLink>*/}
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
                This is a Tech Preview version. You are logged in as an
                anonymous user with read-only access.
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
          Cluster overview
        </NavItemLink>
        <NavItemLink url={`/kafka/${s.id}/topics`}>Topics</NavItemLink>
        <NavItemLink url={`/kafka/${s.id}/nodes`}>Brokers</NavItemLink>
        {/*
      <NavItemLink url={`/kafka/${s.id}/service-registry`}>
        Service registry
      </NavItemLink>
*/}
        <NavItemLink url={`/kafka/${s.id}/consumer-groups`}>
          Consumer groups
        </NavItemLink>
      </NavExpandable>
    ));
}
