import { getKafkaClusters } from "@/api/kafka/actions";
import { ClusterList } from "@/api/kafka/schema";
import { logger } from "@/utils/logger";
import { AuthOptions, Session } from "next-auth";
import { JWT } from "next-auth/jwt";
import { Provider } from "next-auth/providers/index";
import { makeAnonymous } from "./anonymous";
import { makeOauthTokenProvider } from "./oauth-token";
import { makeScramShaProvider } from "./scram";
import oidcSource from "./oidc";

const log = logger.child({ module: "auth" });

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

export async function getAuthOptions(): Promise<AuthOptions> {
  let providers: Provider[];
  log.trace("fetching the oidcSource");
  let oidc = await oidcSource();

  if (oidc.isEnabled()) {
    log.trace("OIDC is enabled");
    providers = [oidc.provider!];
    return {
      providers,
      pages: {
        signIn: "/api/auth/oidc/signin",
      },
      callbacks: {
        async jwt({ token, account }: { token: JWT; account: any }) {
          return oidc.jwt({ token, account });
        },
        async session({ session, token }: { session: Session; token: JWT }) {
          return oidc.session({ session, token });
        },
      },
    };
  } else {
    log.debug("OIDC is disabled");
    // retrieve the authentication method required by the default Kafka cluster
    const clusters = (await getKafkaClusters(true))?.payload;
    providers = clusters?.data.map(makeAuthOption) || [];
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
        async session({ session, token }) {
          // Send properties to the client, like an access_token and user id from a provider.
          session.authorization = token.authorization;

          return session;
        },
      },
    };
  }
}
