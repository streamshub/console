import keycloak from "@/app/api/auth/[...nextauth]/keycloak";
import openshift from "@/app/api/auth/[...nextauth]/openshift";
import { logger } from "@/utils/logger";
import NextAuth, { NextAuthOptions } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";
import { Provider } from "next-auth/providers/index";

const log = logger.child({ module: "auth" });

let _providers: Provider[] = [];
let _callbacks = {};

if (keycloak.isEnabled()) {
  log.debug("Using keycloak provider");
  _providers.push(keycloak.provider as Provider);
  _callbacks = {
    jwt: keycloak.jwt,
    session: keycloak.session,
  };
} else if (openshift.isEnabled()) {
  log.debug("Using OpenShift provider");
  _providers.push(openshift.provider);
} else {
  log.debug("Using anonymous provider");
  _providers.push(CredentialsProvider({
    // The name to display on the sign in form (e.g. 'Sign in with...')
    name: "Anonymous Session",

    credentials: {},

    async authorize() {
      return { id: "1", name: "Anonymous", email: "anonymous@example.com" };
    }
  }));
}

export const authOptions: NextAuthOptions = {
  providers: _providers,
  callbacks: _callbacks,
};

export default NextAuth(authOptions);
