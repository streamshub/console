import { getPrincipals } from "@/api/getPrincipals";
import { getTools } from "@/api/getTools";
import { ApplicationLauncher } from "@/components/applicationLauncher";
import { AppMasthead } from "@/components/appMasthead";
import { PrincipalSelector } from "@/components/principalSelector";
import {
  Page,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { getSession } from "@/utils/session";
import { ReactNode } from "react";

type Props = {
  children: ReactNode;
  mastheadContent: ReactNode;
};

export default async function ToolsLayout({
  children,
  mastheadContent,
}: Props) {
  const session = await getSession();

  const { principalId } = session || {};

  const tools = await getTools();
  const principals = await getPrincipals();
  const selectedPrincipal = principals.find((p) => principalId == p.id);

  return (
    <Page
      header={
        <AppMasthead
          main={
            <Toolbar>
              <ToolbarContent>
                <ToolbarItem>
                  <PrincipalSelector
                    principals={principals}
                    selected={selectedPrincipal}
                  />
                </ToolbarItem>
              </ToolbarContent>
            </Toolbar>
          }
          content={
            <Toolbar>
              <ToolbarContent>
                <ToolbarItem>
                  <ApplicationLauncher tools={tools} />
                </ToolbarItem>
                <ToolbarItem>{mastheadContent}</ToolbarItem>
              </ToolbarContent>
            </Toolbar>
          }
        />
      }
    >
      {children}
    </Page>
  );
}
