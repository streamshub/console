import { getKafkaClusters } from "@/api/kafka/actions";
import { ClusterList } from "@/api/kafka/schema";
import { makeOauthTokenProvider } from "@/app/api/auth/[...nextauth]/oauth-token";
import { logger } from "@/utils/logger";
import NextAuth, { AuthOptions } from "next-auth";
import { Provider } from "next-auth/providers/index";
import { NextRequest, NextResponse } from "next/server";
import { makeAnonymous } from "./anonymous";
import { makeOauthProvider } from "./keycloak";
import { makeScramShaProvider } from "./scram";

const log = logger.child({ module: "auth" });

export async function getAuthOptions(): Promise<AuthOptions> {
  // retrieve the authentication method required by the default Kafka cluster
  const clusters = await getKafkaClusters();
  const providers = clusters.map(makeAuthOption);
  log.trace({ providers }, "getAuthOptions");
  return {
    providers,
    callbacks: {
      async jwt({ token, user }) {
        if (user) {
          token.authorization = user.authorization;
        }
        return token;
      },
      async session({ session, token, user }) {
        // Send properties to the client, like an access_token and user id from a provider.
        session.authorization = token.authorization;

        return session;
      },
    },
  };
}

function makeAuthOption(cluster: ClusterList): Provider {
  switch (cluster.meta.authentication?.method) {
    case "oauth": {
      const { tokenUrl } = cluster.meta.authentication;
      return makeOauthTokenProvider(tokenUrl ?? "TODO");
    }
    case "basic":
      return makeScramShaProvider(cluster.id);
    case "anonymous":
    default:
      return makeAnonymous();
  }
}

// const handler = NextAuth(authOptions);
async function handler(req: NextRequest, res: NextResponse) {
  const authOptions = await getAuthOptions();
  if (authOptions) {
    // set up the auth handler, if undefined there is no authentication required for the cluster
    const authHandler = NextAuth({
      ...authOptions,
      debug: process.env.NODE_ENV === "development",
      // logger: {
      //   debug: (code, ...metadata) => {
      //     log.debug(metadata, code);
      //   },
      //   warn: (code, ...metadata) => {
      //     log.warn(metadata, code);
      //   },
      //   error: (code, ...metadata) => {
      //     log.error(metadata, code);
      //   },
      // },
    });

    // handle the request
    return authHandler(req, res);
  }
}

export { handler as GET, handler as POST };
