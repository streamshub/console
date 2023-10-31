import { getKafkaClusters } from "@/api/kafka";
import { AppMasthead } from "@/app/[locale]/AppMasthead";
import { AppSidebar } from "@/app/[locale]/AppSidebar";
import { NavExpandable } from "@/components/NavExpandable";
import { NavItemLink } from "@/components/NavItemLink";
import { Nav, NavList, Page } from "@/libs/patternfly/react-core";
import { PropsWithChildren, Suspense } from "react";

export async function AppLayout({ children }: PropsWithChildren) {
  const clusters = await getKafkaClusters();
  return (
    <Page
      header={<AppMasthead />}
      sidebar={
        <AppSidebar>
          <Nav aria-label="Nav">
            <NavList>
              <NavItemLink url={"/resources"}>Resources</NavItemLink>
              <NavExpandable
                title={"Kafka clusters"}
                url={"/kafka"}
                startExpanded={true}
              >
                <Suspense>
                  {clusters.map((s, idx) => (
                    <NavExpandable
                      key={s.id}
                      title={s.attributes.name}
                      url={`/kafka/${s.id}`}
                      startExpanded={idx === 0}
                    >
                      <NavItemLink url={`/kafka/${s.id}/overview`}>
                        Cluster overview
                      </NavItemLink>
                      <NavItemLink url={`/kafka/${s.id}/topics`}>
                        Topics
                      </NavItemLink>
                      <NavItemLink url={`/kafka/${s.id}/nodes`}>
                        Nodes
                      </NavItemLink>
                      <NavItemLink url={`/kafka/${s.id}/service-registry`}>
                        Service registry
                      </NavItemLink>
                      <NavItemLink url={`/kafka/${s.id}/consumer-groups`}>
                        Consumer groups
                      </NavItemLink>
                    </NavExpandable>
                  ))}
                </Suspense>
              </NavExpandable>
              <NavItemLink url={"/learning-resources"}>
                Learning resources
              </NavItemLink>
            </NavList>
          </Nav>
        </AppSidebar>
      }
    >
      {children}
    </Page>
  );
}
