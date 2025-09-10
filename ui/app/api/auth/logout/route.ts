import { NextRequest, NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";
import oidcSource from "../[...nextauth]/oidc";
import { oidcEnabled } from "@/utils/config";

export async function GET(req: NextRequest) {
  const isOidc = await oidcEnabled();

  if (!isOidc) {
    return NextResponse.redirect(new URL("/logout", req.url));
  }

  const token = await getToken({ req });
  const oidc = await oidcSource();
  const logoutUrl = await oidc.getLogoutUrl();

  if (!logoutUrl || !token?.id_token) {
    return NextResponse.redirect("/");
  }

  const url = new URL(logoutUrl);
  url.searchParams.set("id_token_hint", token.id_token);
  url.searchParams.set(
    "post_logout_redirect_uri",
    new URL("/api/auth/oidc/signin", req.url).toString(),
  );

  return NextResponse.redirect(url.toString());
}
