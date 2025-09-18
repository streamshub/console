"use client";

import { signOut, getSession } from "next-auth/react";
import oidcSource from "@/app/api/auth/[...nextauth]/oidc";

export async function handleLogout() {
  if (typeof window === "undefined") return;

  const oidc = await oidcSource();

  if (oidc.isEnabled()) {
    const session = await getSession();
    const idToken = session?.idToken;

    const discovery = await fetch(`${oidc.provider!.wellKnown}`);
    const discoveryDoc = await discovery.json();

    const endSessionEndpoint = discoveryDoc.end_session_endpoint;
    if (endSessionEndpoint && idToken) {
      const logoutUrl = `${endSessionEndpoint}?id_token_hint=${encodeURIComponent(
        idToken,
      )}&post_logout_redirect_uri=${encodeURIComponent(
        window.location.origin + "/api/auth/oidc/signin",
      )}`;

      // Clear local session without redirect flicker
      await signOut({ redirect: false });

      // Then navigate browser to Keycloak logout page
      window.location.href = logoutUrl;
      return;
    }
  }

  return signOut({ callbackUrl: "/" });
}
