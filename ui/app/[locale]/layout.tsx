import { AppLayout } from "@/app/[locale]/AppLayout";
import { authOptions } from "@/app/api/auth/[...nextauth]/route";
import { getUser } from "@/utils/session";
import { getServerSession } from "next-auth";
import { NextIntlClientProvider } from "next-intl";
import { getTranslator } from "next-intl/server";
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
        <NextIntlClientProvider locale={locale} messages={messages}>
          <AppLayout session={session}>{children}</AppLayout>
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

// export function generateStaticParams() {
//   return [{ locale: "en" }];
// }
