import { keycloak, refreshToken } from "@/app/api/auth/[...nextauth]/keycloak";
import CredentialsProvider from "next-auth/providers/credentials";

import { logger } from "@/utils/logger";
import NextAuth, { NextAuthOptions, Session } from "next-auth";
import { JWT } from "next-auth/jwt";

const log = logger.child({ module: "auth" });

const anonymousProvider = CredentialsProvider({
  // The name to display on the sign in form (e.g. 'Sign in with...')
  name: 'Anonymous Session',

  credentials: { },

  async authorize() {
    return { id: '1', name: 'Anonymous', email: 'anonymous@example.com' };
  }
});

let _providers = [];
let _callbacks = {};

if (keycloak.options?.issuer) {
  log.debug("Using keycloak provider");
  _providers.push(keycloak);
  _callbacks = {
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

      return refreshToken(token);
    },
    async session({ session, token }: { session: Session, token: JWT }) {
      // Send properties to the client, like an access_token from a provider.
      log.trace(token, "Creating session from token");
      return {
        ...session,
        error: token.error,
        accessToken: token.access_token,
      };
    },
  };
} else {
  log.debug("Using anonymous provider");
  _providers.push(anonymousProvider);
}

export const authOptions: NextAuthOptions = {
  providers: _providers,
  callbacks: _callbacks,
};

export default NextAuth(authOptions);
