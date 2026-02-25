"use client";
import { AbstractIntlMessages, NextIntlClientProvider } from "next-intl";
import { ReactNode, useState, useEffect } from "react";

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
  // Use UTC as default for SSR to avoid hydration mismatch
  // Client will update to actual timezone after mount
  const [timeZone, setTimeZone] = useState<string>("UTC");

  useEffect(() => {
    setTimeZone(Intl.DateTimeFormat().resolvedOptions().timeZone);
  }, []);

  return (
    <NextIntlClientProvider
      locale={locale}
      messages={messages}
      timeZone={timeZone}
    >
      {children}
    </NextIntlClientProvider>
  );
}
