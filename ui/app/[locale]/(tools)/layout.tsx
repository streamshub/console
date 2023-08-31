import { getAuthProfiles } from "@/api/getAuthProfiles";
import { getTools } from "@/api/getTools";
import { authOptions } from "@/app/api/auth/[...nextauth]/route";
import { ApplicationLauncher } from "@/components/applicationLauncher";
import { AppMasthead } from "@/components/appMasthead";
import { PrincipalSelector } from "@/components/principalSelector";
import {
  Page,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { getSession, getUser } from "@/utils/session";
import { getServerSession } from "next-auth";
import { ReactNode } from "react";

type Props = {
  children: ReactNode;
  mastheadContent: ReactNode;
};

export default async function ToolsLayout({
  children,
  mastheadContent,
}: Props) {
  const user = await getUser();
  const session = await getSession();

  const { principalId } = session || {};

  const tools = await getTools();
  const principals = await getAuthProfiles();

  return (
    <Page
      header={
        <AppMasthead
          username={user.username}
          content={
            <Toolbar>
              <ToolbarContent>
                <ToolbarItem>
                </ToolbarItem>
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
