import { AppMasthead } from "@/components/appMasthead";
import {
  Page,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { getUser } from "@/utils/session";
import { PropsWithChildren, ReactNode } from "react";

export default async function Layout({
  children,
  breadcrumbs,
}: PropsWithChildren<{ breadcrumbs: ReactNode }>) {
  const user = await getUser();

  return (
    <Page
      header={
        <AppMasthead
          username={user.username}
          content={
            <Toolbar id="auth-profile-toolbar">
              <ToolbarContent>
                <ToolbarItem>{breadcrumbs}</ToolbarItem>
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
