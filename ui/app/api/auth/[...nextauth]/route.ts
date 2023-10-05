import { logger } from "@/utils/logger";
import NextAuth, { AuthOptions, type TokenSet } from "next-auth";
import KeycloakProvider from "next-auth/providers/keycloak";

const log = logger.child({ module: "auth-route" });

const keycloak = KeycloakProvider({
  clientId: process.env.KEYCLOAK_CLIENTID,
  clientSecret: process.env.KEYCLOAK_CLIENTSECRET,
  issuer: process.env.KEYCLOAK_ISSUER,
});

const tokenEndpoint = { value: "" };

if (keycloak.wellKnown) {
  fetch(keycloak.wellKnown)
    .then((resp) => resp.json())
    .then((json) => {
      tokenEndpoint.value = json.token_endpoint;
    });
}

export const authOptions: AuthOptions = {
  providers: [keycloak],
  callbacks: {
    async jwt({ token, account }) {
      // Persist the OAuth access_token and or the user id to the token right after signin
      if (account) {
        log.trace("account present, saving new token");
        // Save the access token and refresh token in the JWT on the initial login
        return {
          access_token: account.access_token,
          expires_at: account.expires_at,
          refresh_token: account.refresh_token,
        };
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
        // If the access token has not expired yet, return it
        let refresh_token =
          typeof token.refresh_token === "string" ? token.refresh_token : "";

        const params = {
          client_id: keycloak.options?.clientId ?? "",
          client_secret: keycloak.options?.clientSecret ?? "",
          grant_type: "refresh_token",
          refresh_token: refresh_token,
        };

        log.trace(
          {
            url: tokenEndpoint.value,
            body: params,
          },
          "Refreshing token",
        );

        try {
          const response = await fetch(tokenEndpoint.value, {
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams(params),
            method: "POST",
          });

          const refreshToken: TokenSet = await response.json();
          if (!response.ok) {
            throw refreshToken;
          }
          log.trace(refreshToken, "Got refresh token");

          let expires_in =
            typeof refreshToken.expires_in === "number"
              ? refreshToken.expires_in
              : -1;

          const newToken = {
            ...token, // Keep the previous token properties
            access_token: refreshToken.access_token,
            expires_at: Math.floor(Date.now() / 1000 + expires_in),
            // Fall back to old refresh token, but note that
            // many providers may only allow using a refresh token once.
            refresh_token: refreshToken.refresh_token ?? token.refresh_token,
          };
          log.trace(newToken, "New token");
          return newToken;
        } catch (error) {
          log.error(error, "Error refreshing access token");
          // The error property will be used client-side to handle the refresh token error
          return { ...token, error: "RefreshAccessTokenError" as const };
        }
      }
    },
    async session({ session, token }) {
      // Send properties to the client, like an access_token from a provider.
      return {
        ...session,
        error: session.error,
        accessToken: token.access_token,
      };
    },
  },
};

const handler = NextAuth(authOptions);

export { handler as GET, handler as POST };
