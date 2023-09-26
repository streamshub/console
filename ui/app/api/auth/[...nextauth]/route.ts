import NextAuth, { AuthOptions } from "next-auth";
import KeycloakProvider from "next-auth/providers/keycloak";

const users = ["developer", "admin"];

export const authOptions: AuthOptions = {
  providers: [
    KeycloakProvider({
      clientId: process.env.KEYCLOAK_CLIENTID,
      clientSecret: process.env.KEYCLOAK_CLIENTSECRET,
      issuer: process.env.KEYCLOAK_ISSUER,
    }),
  ],
  callbacks: {
    async jwt({ token, account, profile }) {
      // Persist the OAuth access_token and or the user id to the token right after signin
      if (account) {
        token.accessToken = account.access_token;
      }
      return token;
    },
    async session({ session, token }) {
      // Send properties to the client, like an access_token from a provider.
      session.accessToken = token.accessToken;
      return session;
    },
  },
};

const handler = NextAuth(authOptions);

export { handler as GET, handler as POST };
