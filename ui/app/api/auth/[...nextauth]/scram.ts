import { AuthOptions } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";

export function makeScramShaProvider(): AuthOptions {
  const provider = CredentialsProvider({
    // The name to display on the sign in form (e.g. 'Sign in with...')
    name: "Kafka SAML",

    credentials: {
      username: { label: "Username", type: "text" },
      password: { label: "Password", type: "password" },
    },

    async authorize() {
      // TODO
      // try the username/password combo against the getKafkaCluster API call
      // if we get a response, then we can assume the credentials are correct
      if (false) {
        return { id: "1", name: "SCRAM", email: "scram@example.com" };
      }
      // store the credentials in the session
      // if we didn't get a successful response, the credentials are wrong
      return null;
    },
  });

  return {
    providers: [provider],
    callbacks: {},
  };
}
