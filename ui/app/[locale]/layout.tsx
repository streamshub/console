import "../globals.css";
import { getTools } from "@/app/[locale]/_api/getTools";
import { Masthead } from "@/app/[locale]/_components/masthead";
import { Header } from "./_components/header";
import { Page } from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";
import { NextIntlClientProvider, useLocale } from "next-intl";
import { ReactNode } from "react";
import { getTranslator } from "next-intl/server";

type Props = {
  children: ReactNode;
  params: { locale: string };
};

export default async function RootLayout({
  children,
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
          <Page header={<Masthead tools={tools} />}>
            <Header />
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
  return [{ locale: "en" }, { locale: "de" }];
}
