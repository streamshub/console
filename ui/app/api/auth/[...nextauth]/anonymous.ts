import { AuthOptions } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";
import { Provider } from "next-auth/providers/index";

export function makeAnonymous(): Provider {
  const provider = CredentialsProvider({
    name: "Unauthenticated",
    credentials: {},
    async authorize() {
      return { id: "1", name: "Anonymous", email: "anonymous@example.com" };
    },
  });

  return provider;
}
