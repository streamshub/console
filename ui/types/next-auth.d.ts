import NextAuth, { DefaultSession } from "next-auth";

declare module "next-auth" {
  /**
   * Returned by `useSession`, `getSession` and received as a prop on the `SessionProvider` React Context
   */
  interface Session {
    error?: "RefreshAccessTokenError";
    accessToken?: string;
    basicAuth?: string;
    user?: {
      name: string;
      email?: string | null;
      picture?: string | null;
    } & DefaultSession["user"];
  }

  interface User {
    basicAuth?: string;
  }
}

declare module "next-auth/jwt" {
  /** Returned by the `jwt` callback and `getToken`, when using JWT sessions */
  interface JWT {
    accessToken?: string;
    basicAuth?: string;
  }
}
