import CredentialsProvider from "next-auth/providers/credentials";
import { Provider } from "next-auth/providers/index";

export function makeAnonymous(): Provider {
  const provider = CredentialsProvider({
    id: "anonymous",
    credentials: {},
    async authorize() {
      return { id: "1", name: "Anonymous", email: "anonymous@example.com" };
    },
  });

  return provider;
}
