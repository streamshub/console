"use client";

import { defaultTranslationValues } from "@/i18n";
import { AbstractIntlMessages, NextIntlClientProvider } from "next-intl";
import { ReactNode } from "react";

type Props = {
  messages: AbstractIntlMessages;
  locale: string;
  children: ReactNode;
};
export default function NextIntlProvider({
  messages,
  locale,
  children,
}: Props) {
  const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  return (
    <NextIntlClientProvider
      locale={locale}
      messages={messages}
      defaultTranslationValues={defaultTranslationValues}
      now={new Date()}
      timeZone={timeZone}
    >
      {children}
    </NextIntlClientProvider>
  );
}
