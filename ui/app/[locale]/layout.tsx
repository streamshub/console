import { AppLayout } from "@/app/[locale]/AppLayout";
import { AppLayoutProvider } from "@/app/[locale]/AppLayoutProvider";
import { AppSessionProvider } from "@/app/[locale]/AppSessionProvider";
import NextIntlProvider from "@/app/[locale]/NextIntlProvider";
import { SessionRefresher } from "@/app/[locale]/SessionRefresher";
import { authOptions } from "@/app/api/auth/[...nextauth]/route";
import { getServerSession } from "next-auth";
import { getTranslations } from "next-intl/server";
import { notFound } from "next/navigation";
import { ReactNode } from "react";
import "../globals.css";

type Props = {
  children: ReactNode;
  params: { locale: string };
};

export default async function Layout({ children, params: { locale } }: Props) {
  let messages;
  try {
    messages = (await import(`../../messages/${locale}.json`)).default;
  } catch (error) {
    notFound();
  }
  const session = await getServerSession(authOptions);
  return (
    <html lang="en">
      <body>
        <NextIntlProvider locale={locale} messages={messages}>
          <AppLayoutProvider>
            <AppSessionProvider session={session}>
              <AppLayout>{children}</AppLayout>
              <SessionRefresher />
            </AppSessionProvider>
          </AppLayoutProvider>
        </NextIntlProvider>
      </body>
    </html>
  );
}

export async function generateMetadata({
  params: { locale },
}: Omit<Props, "children">) {
  const t = await getTranslations({ locale, namespace: "common" });

  return {
    title: t("title"),
  };
}

// export function generateStaticParams() {
//   return [{ locale: "en" }];
// }
