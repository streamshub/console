import { getMessages, getTranslations } from "next-intl/server";
import { ReactNode } from "react";
import NextIntlProvider from "./NextIntlProvider";
import "../globals.css";

type Props = {
  children: ReactNode;
  params: Promise<{ locale: string }>;
};

export default async function Layout({ children, params: paramsPromise }: Props) {
  const { locale } = await paramsPromise;
  const messages = await getMessages();
  return (
    <NextIntlProvider locale={locale} messages={messages}>
      {children}
    </NextIntlProvider>
  );
}

export async function generateMetadata({
  params: paramsPromise,
}: Omit<Props, "children">) {
  const { locale } = await paramsPromise;
  const t = await getTranslations({ locale, namespace: "common" });

  return {
    title: t("title"),
  };
}
