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
      let tokenExpiration = new Date(
        (typeof token?.expires_at === "number" ? token.expires_at : 0) * 1000,
      );
      log.debug("Token expiration, expires_at:", tokenExpiration);

      // Persist the OAuth access_token and or the user id to the token right after signin
      if (account) {
        log.debug("account present, saving new token");
        // Save the access token and refresh token in the JWT on the initial login
        return {
          access_token: account.access_token,
          expires_at: account.expires_at,
          refresh_token: account.refresh_token,
        };
      }

      if (Date.now() < tokenExpiration.getTime()) {
        log.debug("Token not yet expired");
        // If the access token has not expired yet, return it
        return token;
      } else {
        log.debug("Token has expired");
        let refresh_token =
          typeof token.refresh_token === "string" ? token.refresh_token : "";

        const params = new URLSearchParams({
          client_id: keycloak.options?.clientId ?? "",
          client_secret: keycloak.options?.clientSecret ?? "",
          grant_type: "refresh_token",
          refresh_token: refresh_token,
        });

        try {
          const response = await fetch(tokenEndpoint.value, {
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: params,
            method: "POST",
          });

          const tokens: TokenSet = await response.json();

          if (!response.ok) {
            throw tokens;
          }

          let expires_in =
            typeof tokens.expires_in === "number" ? tokens.expires_in : -1;

          return {
            ...token, // Keep the previous token properties
            access_token: tokens.access_token,
            expires_at: Math.floor(Date.now() / 1000 + expires_in),
            // Fall back to old refresh token, but note that
            // many providers may only allow using a refresh token once.
            refresh_token: tokens.refresh_token ?? token.refresh_token,
          };
        } catch (error) {
          log.error("Error refreshing access token", error);
          // The error property will be used client-side to handle the refresh token error
          return { ...token, error: "RefreshAccessTokenError" as const };
        }
      }
    },
    async session({ session, token }) {
      // Send properties to the client, like an access_token from a provider.
      return {
        ...session,
        accessToken: token.access_token,
      };
    },
  },
};

const handler = NextAuth(authOptions);

export { handler as GET, handler as POST };
