"use client";

import { AppMasthead } from "@/app/[locale]/AppMasthead";
import { SessionRefresher } from "@/app/[locale]/SessionRefresher";
import { NavItemLink } from "@/components/NavItemLink";
import {
  Nav,
  NavList,
  Page,
  PageSidebar,
  PageSidebarBody,
} from "@/libs/patternfly/react-core";
import { Session } from "next-auth";
import { SessionProvider } from "next-auth/react";
import { PropsWithChildren, useState } from "react";
import { logger } from "@/utils/loggerClient";

const log = logger.child({ module: "ui", component: "AppLayout" });

export function AppLayout({
  session,
  children,
}: PropsWithChildren<{ session: Session }>) {
  const [sidebarExpanded, setSidebarExpanded] = useState(true);
  return (
    <SessionProvider session={session}>
      <Page
        header={
          <AppMasthead onToggleSidebar={() => setSidebarExpanded((e) => !e)} />
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
      <SessionRefresher />
    </SessionProvider>
  );
}
