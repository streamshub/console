import { type Session } from "next-auth";
import { SessionProvider } from "next-auth/react";
import { NextIntlClientProvider } from "next-intl";
import { type AppType } from "next/app";

import "~/styles/globals.css";

import { api } from "~/utils/api";

const MyApp: AppType<{ session: Session | null; messages: IntlMessages }> = ({
  Component,
  pageProps: { session, ...pageProps },
}) => {
  return (
    <NextIntlClientProvider locale={"en"} messages={pageProps.messages}>
      <SessionProvider session={session}>
        <Component {...pageProps} />
      </SessionProvider>
    </NextIntlClientProvider>
  );
};

export default api.withTRPC(MyApp);
