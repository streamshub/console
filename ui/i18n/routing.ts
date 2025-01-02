import { defineRouting } from "next-intl/routing";
import { createNavigation } from "next-intl/navigation";

export const locales = ["en"] as const;

export const routing = defineRouting({
  // A list of all locales that are supported
  locales,

  // Used when no locale matches
  defaultLocale: "en",
  localePrefix: "never",
});

// Lightweight wrappers around Next.js' navigation APIs
// that will consider the routing configuration
let nav = createNavigation(routing);

export const { Link, redirect, usePathname, useRouter } = {
    Link: nav.Link,
    redirect: (path: string) => {
        nav.redirect({ href: path, locale: "en" });
    },
    usePathname: nav.usePathname,
    useRouter: nav.useRouter,
};
