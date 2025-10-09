import { logger } from "@/utils/logger";
import { Session, TokenSet } from "next-auth";
import { JWT } from "next-auth/jwt";
import { OAuthConfig } from "next-auth/providers/index";
import config from "@/utils/config";

const log = logger.child({ module: "oidc" });

class OpenIdConnect {
  provider: OAuthConfig<any> | null;

  constructor(
    authServerUrl: string | null,
    clientId: string | null,
    clientSecret: string | null,
    truststore: string | null,
    scopes: string | null,
  ) {
    if (clientId && clientSecret && authServerUrl) {
      this.provider = {
        id: "oidc",
        name: "OpenID Connect Provider",
        type: "oauth",
        clientId: clientId,
        clientSecret: clientSecret,
        wellKnown: `${authServerUrl}/.well-known/openid-configuration`,
        authorization: {
          params: { scope: scopes || "openid email profile groups" },
        },
        idToken: true,
        profile(profile) {
          return {
            id: profile.sub,
            name: profile.name ?? profile.preferred_username,
            email: profile.email,
            image: profile.image,
          };
        },
        httpOptions: {
          ca: truststore ?? undefined,
        },
      };
    } else {
      this.provider = null;
    }
  }

  isEnabled() {
    return this.provider != null;
  }

  async getTokenEndpoint() {
    const discoveryEndpoint: string = this.provider!.wellKnown!;
    let _tokenEndpoint: string | undefined = undefined;

    log.trace(`wellKnown endpoint: ${discoveryEndpoint}`);
    const response = await fetch(discoveryEndpoint);
    const discovery = await response.json();

    _tokenEndpoint = discovery.token_endpoint;
    log.trace(`token endpoint: ${_tokenEndpoint}`);

    return _tokenEndpoint;
  }

  isExpired(token: JWT): boolean {
    let tokenExpiration = new Date(
      (typeof token?.expires_at === "number" ? token.expires_at : 0) * 1000,
    );

    let remainingMs = tokenExpiration.getTime() - Date.now();

    if (remainingMs > 30000) {
      log.trace(
        `Token expires at ${tokenExpiration.toISOString()}, time remaining: ${remainingMs}ms`,
      );
      return false;
    }

    if (remainingMs > 0) {
      log.trace(
        `Token expires at ${tokenExpiration.toISOString()}, time remaining: ${remainingMs}ms (less than 30s)`,
      );
    } else {
      log.trace(`Token expired at ${tokenExpiration.toISOString()}`);
    }

    return true;
  }

  async refreshToken(token: JWT): Promise<JWT> {
    let refresh_token =
      typeof token.refresh_token === "string" ? token.refresh_token : undefined;

    if (refresh_token === undefined) {
      return {
        error: "Refresh token not available, expiring session",
      };
    }

    const params = {
      client_id: this.provider!.clientId!,
      client_secret: this.provider!.clientSecret!,
      grant_type: "refresh_token",
      refresh_token: refresh_token,
    };

    const tokenEndpoint = await this.getTokenEndpoint();

    if (!tokenEndpoint) {
      return {
        error: "Invalid OIDC wellKnown",
      };
    }

    log.trace(
      { url: tokenEndpoint, params: params },
      "Refreshing token",
    );

    const response = await fetch(tokenEndpoint, {
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams(params),
      method: "POST",
    });

    const responseBody = await response.text();

    if (!response.ok) {
      log.debug({ responseBody }, "Bad token response");
      return {
        error: responseBody,
      };
    }

    const refreshToken: TokenSet = JSON.parse(responseBody);
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

  async jwt({ token, account }: { token: JWT; account: any }) {
    // Persist the OAuth access_token and or the user id to the token right after signin
    log.trace("jwt callback invoked");

    if (account) {
      log.trace({ account, token }, `account present, saving new token`);

      // Save the access token and refresh token in the JWT on the initial login
      return {
        access_token: account.access_token,
        expires_at: account.expires_at,
        refresh_token: account.refresh_token,
        email: token.email,
        name: token.name,
        picture: token.picture,
        sub: token.sub,
        id_token: account.id_token,
      };
    }

    if (this.isExpired(token)) {
      return this.refreshToken(token);
    }

    return token;
  }

  async session({ session, token }: { session: Session; token: JWT }) {
    if (token.error) {
      session.expires = new Date(0).toISOString();
      return session;
    }

    // Send properties to the client, like an access_token from a provider.
    log.trace(token, "Updating session with token");
    return {
      ...session,
      error: token.error,
      accessToken: token.access_token,
      idToken: token.id_token,
      authorization: `Bearer ${token.access_token}`,
    };
  }
}

export default async function oidcSource() {
  let oidcConfig = (await config())?.security?.oidc;

  return new OpenIdConnect(
    oidcConfig?.authServerUrl ?? null,
    oidcConfig?.clientId ?? null,
    oidcConfig?.clientSecret ?? null,
    oidcConfig?.trustStore?.content?.value ?? null,
    oidcConfig?.scopes ?? null,
  );
}
