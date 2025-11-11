"use client";

import { signOut, getSession } from "next-auth/react";
import oidcSource from "@/app/api/auth/[...nextauth]/oidc";

export async function handleLogout() {
  if (typeof window === "undefined") return;

  const oidc = await oidcSource();

  try {
    // Check if OIDC is configured
    if (oidc.isEnabled()) {
      const session = await getSession();
      const idToken = session?.idToken;

      const discovery = await fetch(`${oidc.provider!.wellKnown}`);
      const discoveryDoc = await discovery.json();

      const endSessionEndpoint = discoveryDoc.end_session_endpoint;

      if (endSessionEndpoint) {
        // Build logout URL (with or without id_token_hint)
        const logoutUrl = `${endSessionEndpoint}${
          idToken ? `?id_token_hint=${encodeURIComponent(idToken)}&` : "?"
        }post_logout_redirect_uri=${encodeURIComponent(
          window.location.origin + "/api/auth/oidc/signin",
        )}`;

        // Sign out locally but don’t redirect immediately
        await signOut({ redirect: false });

        // Redirect to OIDC provider logout
        window.location.href = logoutUrl;
        return;
      }
    }
  } catch (err) {
    console.error("OIDC logout failed, falling back to default logout:", err);
  }

  // Default fallback if OIDC isn't enabled or fails
  return signOut({ callbackUrl: "/" });
}
