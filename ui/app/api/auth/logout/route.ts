// ui/app/api/auth/oidc/logout/route.ts
import { NextRequest, NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";
import oidcSource from "../[...nextauth]/oidc";
import { oidcEnabled } from "@/utils/config";

export async function GET(req: NextRequest) {
  const token = await getToken({ req });
  const isOidc = await oidcEnabled();

  if (!isOidc) {
    // No OIDC → do local logout, redirect to home
    return NextResponse.redirect(new URL("/logout", req.url));
  }

  const oidc = await oidcSource();
  const logoutUrl = await oidc.getLogoutUrl();

  // If discovery failed OR we don't have an id_token → fallback to local logout but redirect to OIDC signin after
  if (!logoutUrl || !token?.id_token) {
    return NextResponse.redirect(
      new URL("/logout?redirect=/api/auth/oidc/signin", req.url),
    );
  }

  // Otherwise do a proper OIDC end-session redirect
  const url = new URL(logoutUrl);
  url.searchParams.set("id_token_hint", token.id_token);
  url.searchParams.set(
    "post_logout_redirect_uri",
    new URL("/api/auth/oidc/signin", req.url).toString(),
  );

  return NextResponse.redirect(url.toString());
}
