import { Page } from "@/libs/patternfly/react-core";
import { NextIntlClientProvider } from "next-intl";
import { getTranslator } from "next-intl/server";
import { notFound } from "next/navigation";
import { ReactNode } from "react";
import "../globals.css";
import { getTools } from "./_api/getTools";
import { AppMasthead } from "./_components/appMasthead";

type Props = {
  children: ReactNode;
  params: { locale: string };
  toolbar: ReactNode;
};

export default async function RootLayout({
  children,
  toolbar,
  params: { locale },
}: Props) {
  let messages;
  try {
    messages = (await import(`../../messages/${locale}.json`)).default;
  } catch (error) {
    notFound();
  }

  const tools = await getTools();

  return (
    <html lang="en">
      <body>
        <NextIntlClientProvider locale={locale} messages={messages}>
          <Page header={<AppMasthead tools={tools} toolbar={toolbar} />}>
            {children}
          </Page>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}

export async function generateMetadata({
  params: { locale },
}: Omit<Props, "children">) {
  const t = await getTranslator(locale, "common");

  return {
    title: t("title"),
  };
}

export function generateStaticParams() {
  return [{ locale: "en" }];
}
