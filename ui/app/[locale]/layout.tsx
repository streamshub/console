import { getAuthOptions } from "@/app/api/auth/[...nextauth]/route";

import { getServerSession } from "next-auth";
import { getTranslations } from "next-intl/server";
import { notFound } from "next/navigation";
import { ReactNode } from "react";
import NextIntlProvider from "./NextIntlProvider";
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
  const authOptions = await getAuthOptions();
  const session = await getServerSession(authOptions);
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
