import { getMessages, getTranslations } from "next-intl/server";
import { useNow, useTimeZone } from "next-intl";
import { ReactNode } from "react";
import NextIntlProvider from "./NextIntlProvider";
import "../globals.css";

type Props = {
  children: ReactNode;
  params: { locale: string };
};

export default async function Layout({ children, params: { locale } }: Props) {
  const messages = await getMessages();
  return (
    <NextIntlProvider locale={locale} messages={messages}>
      {children}
    </NextIntlProvider>
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
