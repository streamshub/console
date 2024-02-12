import { logger } from "@/utils/logger";
import { Session, TokenSet } from "next-auth";
import { JWT } from "next-auth/jwt";
import KeycloakProvider from "next-auth/providers/keycloak";

const log = logger.child({ module: "keycloak" });

const { CONSOLE_OAUTH_CLIENT_ID, CONSOLE_OAUTH_CLIENT_SECRET, CONSOLE_OAUTH_ISSUER } =
  process.env;

class Keycloak {
  provider = CONSOLE_OAUTH_CLIENT_ID && CONSOLE_OAUTH_CLIENT_SECRET && CONSOLE_OAUTH_ISSUER
    ? KeycloakProvider({
        clientId: CONSOLE_OAUTH_CLIENT_ID,
        clientSecret: CONSOLE_OAUTH_CLIENT_SECRET,
        issuer: CONSOLE_OAUTH_ISSUER,
      })
    : null;

  isEnabled() {
    return process.env.CONSOLE_OAUTH_PROVIDER === "keycloak"
      && this.provider !== null;
  }

  async getTokenEndpoint() {
    let _tokenEndpoint: string | undefined = undefined;

    if (this.provider?.wellKnown) {
      const kc = await fetch(this.provider.wellKnown);
      const res = await kc.json();
      _tokenEndpoint = res.token_endpoint;
    }

    return _tokenEndpoint;
  }

  async refreshToken(token: JWT): Promise<JWT> {
    if (this.provider == null) {
      throw new Error("Keycloak is not properly configured");
    }

    try {
      const tokenEndpoint = await this.getTokenEndpoint();
      if (!tokenEndpoint) {
        log.error("Invalid Keycloak wellKnown");
        throw token;
      }
      let tokenExpiration = new Date(
        (typeof token?.expires_at === "number" ? token.expires_at : 0) * 1000,
      );
      log.trace({ tokenExpiration }, "Token expiration");

      if (Date.now() < tokenExpiration.getTime()) {
        log.trace(token, "Token not yet expired");
        return token;
      }
      log.trace(token, "Token has expired");
      let refresh_token =
        typeof token.refresh_token === "string" ? token.refresh_token : "";

      const params = {
        client_id: this.provider.options!.clientId,
        client_secret: this.provider.options!.clientSecret,
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

  async jwt({ token, account }: { token: JWT, account: any }) {
    // Persist the OAuth access_token and or the user id to the token right after signin
    if (account) {
      log.trace("account present, saving new token");
      // Save the access token and refresh token in the JWT on the initial login
      return {
        access_token: account.access_token,
        expires_at: account.expires_at,
        refresh_token: account.refresh_token,
        email: token.email,
        name: token.name,
        picture: token.picture,
        sub: token.sub,
      };
    }

    return this.refreshToken(token);
  };

  async session({ session, token }: { session: Session, token: JWT }) {
    // Send properties to the client, like an access_token from a provider.
    log.trace(token, "Creating session from token");
    return {
      ...session,
      error: token.error,
      accessToken: token.access_token,
    };
  };
}

const keycloak = new Keycloak();
export default keycloak;
