import { AuthOptions } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";

export function makeAnonymous(): AuthOptions {
  const provider = CredentialsProvider({
    name: "Unauthenticated",
    credentials: {},
    async authorize() {
      return { id: "1", name: "Anonymous", email: "anonymous@example.com" };
    },
  });

  return {
    providers: [provider],
    callbacks: {},
  };
}
