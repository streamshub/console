"use client";

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
  return (
    <NextIntlClientProvider
      locale={locale}
      messages={messages}
      defaultTranslationValues={{
        strong: (text) => <strong>{text}</strong>,
      }}
      now={new Date()}
    >
      {children}
    </NextIntlClientProvider>
  );
}
