import { logger } from "@/utils/logger";
import { TokenSet } from "next-auth";
import { JWT } from "next-auth/jwt";
import KeycloakProvider from "next-auth/providers/keycloak";

const log = logger.child({ module: "keycloak" });

const { KEYCLOAK_CLIENTID, KEYCLOAK_CLIENTSECRET, NEXT_PUBLIC_KEYCLOAK_URL } =
  process.env;

export const keycloak =
  KEYCLOAK_CLIENTSECRET && KEYCLOAK_CLIENTID && NEXT_PUBLIC_KEYCLOAK_URL
    ? KeycloakProvider({
        clientId: KEYCLOAK_CLIENTID,
        clientSecret: KEYCLOAK_CLIENTSECRET,
        issuer: NEXT_PUBLIC_KEYCLOAK_URL,
      })
    : undefined;

let _tokenEndpoint: string | undefined = undefined;
async function getTokenEndpoint() {
  if (keycloak && keycloak.wellKnown) {
    const kc = await fetch(keycloak.wellKnown);
    const res = await kc.json();
    _tokenEndpoint = res.token_endpoint;
  }
  return _tokenEndpoint;
}

export async function refreshToken(token: JWT): Promise<JWT> {
  try {
    const tokenEndpoint = await getTokenEndpoint();
    if (!keycloak) {
      log.error("Invalid Keycloak configuratio");
      throw token;
    }
    if (!tokenEndpoint) {
      log.error("Invalid Keycloak wellKnow");
      throw token;
    }
    let tokenExpiration = new Date(
      (typeof token?.expires_at === "number" ? token.expires_at : 0) * 1000,
    );
    log.trace({ tokenExpiration }, "Token expiration");

    if (Date.now() < tokenExpiration.getTime()) {
      log.trace(token, "Token not yet expired");
      return token;
    } else {
      log.trace(token, "Token has expired");
      let refresh_token =
        typeof token.refresh_token === "string" ? token.refresh_token : "";

      const params = {
        client_id: keycloak.options!.clientId,
        client_secret: keycloak.options!.clientSecret,
        grant_type: "refresh_token",
        refresh_token: refresh_token,
      };

      log.trace(
        {
          url: tokenEndpoint,
        },
        "Refreshing token",
      );

      const response = await fetch(tokenEndpoint, {
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams(params),
        method: "POST",
      });

      const refreshToken: TokenSet = await response.json();
      if (!response.ok) {
        throw new Error(response.statusText);
      }
      log.trace(refreshToken, "Got refresh token");

      let expires_in =
        typeof refreshToken.expires_in === "number"
          ? refreshToken.expires_in
          : -1;

      const newToken: JWT = {
        ...token, // Keep the previous token properties
        access_token: refreshToken.access_token,
        expires_at: Math.floor(Date.now() / 1000 + expires_in),
        // Fall back to old refresh token, but note that
        // many providers may only allow using a refresh token once.
        refresh_token: refreshToken.refresh_token ?? token.refresh_token,
      };
      log.trace(newToken, "New token");
      return newToken;
    }
  } catch (error: unknown) {
    if (typeof error === "string") {
      log.error({ message: error }, "Error refreshing access token");
    } else if (error instanceof Error) {
      log.error(error, "Error refreshing access token");
    } else {
      log.error("Unknown error refreshing access token");
    }
    // The error property will be used client-side to handle the refresh token error
    return { ...token, error: "RefreshAccessTokenError" as const };
  }
}
