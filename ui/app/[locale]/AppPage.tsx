"use client";

import { AppMasthead } from "@/app/[locale]/AppMasthead";
import { NavItemLink } from "@/components/NavItemLink";
import {
  Nav,
  NavList,
  Page,
  PageSidebar,
  PageSidebarBody,
} from "@/libs/patternfly/react-core";
import { PropsWithChildren, useState } from "react";

export function AppPage({
  username,
  children,
}: PropsWithChildren<{ username: string }>) {
  const [sidebarExpanded, setSidebarExpanded] = useState(true);
  return (
    <Page
      header={
        <AppMasthead
          username={username}
          onToggleSidebar={() => setSidebarExpanded((e) => !e)}
        />
      }
      sidebar={
        <PageSidebar isSidebarOpen={sidebarExpanded}>
          <PageSidebarBody>
            <Nav aria-label="Nav">
              <NavList>
                <NavItemLink url={"/"} exact={true}>
                  Overview
                </NavItemLink>
                <NavItemLink url={"/resources"}>All resources</NavItemLink>
                <NavItemLink url={"/flows"}>Flows</NavItemLink>
                <NavItemLink url={"/kafka"}>Kafka</NavItemLink>
                <NavItemLink url={"/connectors"}>Connectors</NavItemLink>
                <NavItemLink url={"/registry"}>Registry</NavItemLink>
                <NavItemLink url={"/learning-resources"}>
                  Learning resources
                </NavItemLink>
              </NavList>
            </Nav>
          </PageSidebarBody>
        </PageSidebar>
      }
    >
      {children}
    </Page>
  );
}
