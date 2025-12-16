import { getMessages, getTranslations } from "next-intl/server";
import { ReactNode } from "react";
import NextIntlProvider from "./NextIntlProvider";
import "../globals.css";

type Props = {
  children: ReactNode;
  params: { locale: string };
};

export default async function Layout({ children, params: { locale } }: Props) {
  const messages = await getMessages({ locale });

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
