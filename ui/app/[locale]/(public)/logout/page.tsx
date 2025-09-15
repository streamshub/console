"use client";

import { useEffect } from "react";
import { signOut } from "next-auth/react";

/**
 * LogoutLocal is used for non-OIDC logout or as a fallback when OIDC logout fails.
 *
 * When redirected to this page, it immediately calls `signOut()` from next-auth
 * to clear the session and redirect the user to the homepage (or another callback URL).
 *
 * This is typically triggered when OIDC is disabled or when an id_token is unavailable.
 */

export default function LogoutLocal() {
  useEffect(() => {
    signOut({ callbackUrl: "/" });
  }, []);

  return null;
}
