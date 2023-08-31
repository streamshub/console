import NextAuth from "next-auth";
import CredentialsProvider from 'next-auth/providers/credentials'
import GitlabProvider from 'next-auth/providers/gitlab'
import GithubProvider from 'next-auth/providers/github'
import KeycloakProvider from 'next-auth/providers/keycloak'

const users = ['developer', 'admin']

export const authOptions = {
  providers: [
    CredentialsProvider({
      name: 'Credentials',
      credentials: {
        username: { label: "Username", type: "text" },
        password: { label: "Password", type: "password" }
      },
      async authorize(credentials, req) {

        if (credentials?.username && credentials?.password && users.includes(credentials.username)) {
          return {
            name: credentials.username
          }
        }
        return null;
      }
    }),
    GitlabProvider({}),
    GithubProvider({}),
    KeycloakProvider({})
  ]
}

const handler = NextAuth(authOptions)

export { handler as GET, handler as POST };
