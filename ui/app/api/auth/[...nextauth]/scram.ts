import { getKafkaClusters } from "@/api/kafka/actions";
import { AuthOptions } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";

function bytesToBase64(bytes: Uint8Array): string {
  const binString = Array.from(bytes, (byte) =>
    String.fromCodePoint(byte),
  ).join("");
  return btoa(binString);
}

export function makeScramShaProvider(): AuthOptions {
  const provider = CredentialsProvider({
    // The name to display on the sign in form (e.g. 'Sign in with...')
    name: "Kafka SAML",

    credentials: {
      username: { label: "Username", type: "text" },
      password: { label: "Password", type: "password" },
    },

    async authorize(credentials) {
      // try the username/password combo against the getKafkaCluster API call
      // if we get a response, then we can assume the credentials are correct
      try {
        const clusters = await getKafkaClusters();
        const defaultCluster = clusters[0];
        const url = `${process.env.BACKEND_URL}/api/kafkas/${defaultCluster.id}?$`;
        const basicAuth = bytesToBase64(
          new TextEncoder().encode(
            `${credentials?.username}:${credentials?.password}`,
          ),
        );
        const res = await fetch(url, {
          headers: {
            Accept: "application/json",
            Authorization: `Basic ${basicAuth}`,
            "Content-Type": "application/json",
          },
          cache: "no-cache",
        });

        if (res.status === 200) {
          return { id: "1", name: credentials!.username, basicAuth };
        }
      } catch {}
      // store the credentials in the session
      // if we didn't get a successful response, the credentials are wrong
      return null;
    },
  });

  return {
    providers: [provider],
    pages: {
      signIn: '/auth/signIn',
    },
    callbacks: {
      async jwt({ token, user }) {
        if (user) {
          token.basicAuth = user.basicAuth;
        }
        return token;
      },
      async session({ session, token, user }) {
        // Send properties to the client, like an access_token and user id from a provider.
        session.accessToken = token.accessToken;
        session.basicAuth = token.basicAuth;

        return session;
      },
    },
  };
}
