import NextAuth, { AuthOptions } from "next-auth";
import KeycloakProvider from "next-auth/providers/keycloak";
import { type TokenSet } from "@auth/core/types";

const keycloak = KeycloakProvider({
  clientId: process.env.KEYCLOAK_CLIENTID,
  clientSecret: process.env.KEYCLOAK_CLIENTSECRET,
  issuer: process.env.KEYCLOAK_ISSUER,
});

const tokenEndpoint = { value: "" };

if (keycloak.wellKnown) {
  fetch(keycloak.wellKnown)
    .then(resp => resp.json())
    .then(json => {
      tokenEndpoint.value = json.token_endpoint;
    })
}

export const authOptions: AuthOptions = {
  providers: [ keycloak ],
  callbacks: {
    async jwt({ token, account }) {
      // Persist the OAuth access_token and or the user id to the token right after signin
      if (account) {
        // Save the access token and refresh token in the JWT on the initial login
        return {
          access_token: account.access_token,
          expires_at: account.expires_at,
          refresh_token: account.refresh_token,
        }
      } else if (Date.now() < token.expires_at * 1000) {
        // If the access token has not expired yet, return it
        return token;
      } else {
        const params = new URLSearchParams({
          client_id: keycloak.options.clientId,
          client_secret: keycloak.options.clientSecret,
          grant_type: "refresh_token",
          refresh_token: token.refresh_token,
        });

        console.debug("refreshing token , expired at", new Date(token.expires_at * 1000));

        try {
          const response = await fetch(tokenEndpoint.value, {
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: params,
            method: "POST",
          });

          const tokens: TokenSet = await response.json()

          if (!response.ok) {
            throw tokens;
          }

          //console.log("tokenset", tokens);

          return {
            ...token, // Keep the previous token properties
            access_token: tokens.access_token,
            expires_at: Math.floor(Date.now() / 1000 + tokens.expires_in),
            // Fall back to old refresh token, but note that
            // many providers may only allow using a refresh token once.
            refresh_token: tokens.refresh_token ?? token.refresh_token,
          }
        } catch (error) {
          console.error("Error refreshing access token", error)
          // The error property will be used client-side to handle the refresh token error
          return { ...token, error: "RefreshAccessTokenError" as const }
        }
      }
    },
    async session({ session, token }) {
      // Send properties to the client, like an access_token from a provider.
      session.accessToken = token.access_token;
      return session;
    },
  },
};

const handler = NextAuth(authOptions);

export { handler as GET, handler as POST };

declare module "@auth/core/types" {
  interface Session {
    error?: "RefreshAccessTokenError"
  }
}

declare module "@auth/core/jwt" {
  interface JWT {
    access_token: string
    expires_at: number
    refresh_token: string
    error?: "RefreshAccessTokenError"
  }
}