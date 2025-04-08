import { Content } from "@/libs/patternfly/react-core";
import { getRequestConfig } from "next-intl/server";
import { IntlConfig } from "use-intl";
import { routing } from "./i18n/routing";

export const defaultTranslationValues: IntlConfig["defaultTranslationValues"] =
  {
    strong: (text) => <strong>{text}</strong>,
    b: (text) => <b>{text}</b>,
    i: (text) => <i>{text}</i>,
    br: () => <br />,
    p: (text) => <p>{text}</p>,
    text: (text) => <Content>{text}</Content>,
  };

export default getRequestConfig(async ({ requestLocale }) => {
  let locale = await requestLocale;

  // Ensure that the incoming locale is valid
  if (!locale || !routing.locales.includes(locale as any)) {
    locale = routing.defaultLocale;
  }

  return {
    messages: (await import(`./messages/${locale}.json`)).default,
    defaultTranslationValues,
    locale,
  };
});
