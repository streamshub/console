import { getMessages, getTranslations } from "next-intl/server";
import { ReactNode } from "react";
import NextIntlProvider from "./NextIntlProvider";
import "../globals.css";

type Props = {
  children: ReactNode;
  params: Promise<{ locale: string }>;
};

export default async function Layout(props: Props) {
  const params = await props.params;

  const {
    locale
  } = params;

  const {
    children
  } = props;

  const messages = await getMessages();
  return (
    <NextIntlProvider locale={locale} messages={messages}>
      {children}
    </NextIntlProvider>
  );
}

export async function generateMetadata(props: Omit<Props, "children">) {
  const params = await props.params;

  const {
    locale
  } = params;

  const t = await getTranslations({ locale, namespace: "common" });

  return {
    title: t("title"),
  };
}
