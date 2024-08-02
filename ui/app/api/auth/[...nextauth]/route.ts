import { getKafkaClusters } from "@/api/kafka/actions";
import { ClusterList } from "@/api/kafka/schema";
import { logger } from "@/utils/logger";
import NextAuth, { AuthOptions } from "next-auth";
import { NextRequest, NextResponse } from "next/server";
import { makeAnonymous } from "./anonymous";
import { makeOauthProvider } from "./keycloak";
import { makeScramShaProvider } from "./scram";

const log = logger.child({ module: "auth" });

export async function getAuthOptions(): Promise<AuthOptions> {
  // retrieve the authentication method required by the default Kafka cluster
  const clusters = await getKafkaClusters();
  const defaultCluster = clusters[0];
  if (!defaultCluster) {
    throw new Error("No cluster");
  }
  // build the correct auth provider
  return makeAuthOption(defaultCluster);
}

function makeAuthOption(cluster: ClusterList): AuthOptions {
  switch (cluster.attributes.authMethod?.method) {
    case "oauth": {
      const { clientId, clientSecret, issuer } = cluster.attributes.authMethod;
      return makeOauthProvider(clientId, clientSecret, issuer);
    }
    case "scram-sha":
      return makeScramShaProvider();
    case "anonymous":
    default:
      return makeAnonymous();
  }
}

// const handler = NextAuth(authOptions);
async function handler(req: NextRequest, res: NextResponse) {
  const authOptions = await getAuthOptions();
  // set up the auth handler, if undefined there is no authentication required for the cluster
  const authHandler = NextAuth({
    ...authOptions,
    debug: process.env.NODE_ENV === "development",
    logger: {
      debug: (code, ...metadata) => {
        log.debug(metadata, code);
      },
      warn: (code, ...metadata) => {
        log.warn(metadata, code);
      },
      error: (code, ...metadata) => {
        log.error(metadata, code);
      },
    },
  });
  // handle the request
  return authHandler(req, res);
}

export { handler as GET, handler as POST };
