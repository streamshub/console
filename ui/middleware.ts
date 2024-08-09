import { locales } from "@/navigation";
import withAuth from "next-auth/middleware";
import createIntlMiddleware from "next-intl/middleware";
import { NextRequest, NextResponse } from "next/server";

import { logger } from "@/utils/logger";

const log = logger.child({ module: "middleware" });

const publicPages = ["/kafka/[^/]+/login", "/cluster", "/"];
const protectedPages = ["/kafka/[^/]+/.*"];

const intlMiddleware = createIntlMiddleware({
  // A list of all locales that are supported
  locales,

  // If this locale is matched, pathnames work without a prefix (e.g. `/about`)
  defaultLocale: "en",
  localePrefix: "never",
});

const authMiddleware = withAuth(
  // Note that this callback is only invoked if
  // the `authorized` callback has returned `true`
  // and not for pages listed in `pages`.
  function onSuccess(req) {
    return intlMiddleware(req);
  },
  {
    callbacks: {
      authorized: ({ token }) => token != null,
    },
    pages: {
      signIn: `/kafka/1/login`,
    },
  },
) as any;

const publicPathnameRegex = RegExp(
  `^(/(${locales.join("|")}))?(${publicPages
    .flatMap((p) => (p === "/" ? ["", "/"] : p))
    .join("|")})/?$`,
  "i",
);

const protectedPathnameRegex = RegExp(
  `^(/(${locales.join("|")}))?(${protectedPages
    .flatMap((p) => (p === "/" ? ["", "/"] : p))
    .join("|")})/?$`,
  "i",
);

export default async function middleware(req: NextRequest) {
  const requestPath = req.nextUrl.pathname;
  const isPublicPage = publicPathnameRegex.test(requestPath);
  const isProtectedPage = protectedPathnameRegex.test(requestPath);

  if (isPublicPage) {
    log.trace({ requestPath: requestPath }, "public page");
    return intlMiddleware(req);
  } else if (isProtectedPage) {
    log.trace({ requestPath: requestPath }, "protected page");
    return (authMiddleware as any)(req);
  } else {
    log.debug({ requestPath: requestPath, publicPathnameRegex: publicPathnameRegex, protectedPathnameRegex: protectedPathnameRegex }, "neither public nor protected!");
    return NextResponse.redirect(new URL("/", req.url));
  }
}

export const config = {
  // Skip all paths that should not be internationalized. This example skips the
  // folders "api", "healthz", "_next" and all files with an extension (e.g. favicon.ico)
  matcher: ["/((?!api|healthz|_next|.*\\..*).*)"],
};
