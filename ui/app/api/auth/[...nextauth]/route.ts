import NextAuth from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";
import GithubProvider from "next-auth/providers/github";
import GitlabProvider from "next-auth/providers/gitlab";
import KeycloakProvider from "next-auth/providers/keycloak";

const users = ["developer", "admin"];

export const authOptions = {
  providers: [
    CredentialsProvider({
      name: "Credentials",
      credentials: {
        username: { label: "Username", type: "text" },
        password: { label: "Password", type: "password" },
      },
      async authorize(credentials, req) {
        return new Promise((resolve, reject) => {
          if (
            credentials?.username &&
            credentials?.password &&
            users.includes(credentials.username)
          ) {
            resolve({
              id: credentials.username,
              name: credentials.username,
            });
          } else {
            resolve(null);
          }
        });
      },
    }),
    GitlabProvider({
      clientId: "id",
      clientSecret: "secret",
    }),
    GithubProvider({
      clientId: "id",
      clientSecret: "secret",
    }),
    KeycloakProvider({
      clientId: "id",
      clientSecret: "secret",
    }),
  ],
};

const handler = NextAuth(authOptions);

export { handler as GET, handler as POST };
