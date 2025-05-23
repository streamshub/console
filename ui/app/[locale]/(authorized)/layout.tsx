import { getAuthOptions } from "@/app/api/auth/[...nextauth]/auth-options";

import { getServerSession } from "next-auth";
import { ReactNode } from "react";
import { AppSessionProvider } from "./AppSessionProvider";
import { SessionRefresher } from "./SessionRefresher";

type Props = {
  children: ReactNode;
  params: { locale: string };
};

export default async function Layout({ children, params: { locale } }: Props) {
  const authOptions = await getAuthOptions();
  const session = await getServerSession(authOptions);
  return (
    <AppSessionProvider session={session}>
      {children}
      <SessionRefresher />
    </AppSessionProvider>
  );
}
