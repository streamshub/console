import { getKafkaClusters } from "@/api/kafka/actions";
import { logger } from "@/utils/logger";
import { AuthOptions } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";
import { Provider } from "next-auth/providers/index";

const log = logger.child({ module: "oauth-token" });

export function makeOauthTokenProvider(tokenUrl: string): Provider {
  const provider = CredentialsProvider({
    // The name to display on the sign in form (e.g. 'Sign in with...')
    name: "OAuth token",
    id: "oauth-token",

    credentials: {
      clientId: { label: "Client ID", type: "text" },
      secret: { label: "Client Secret", type: "password" },
    },

    async authorize(credentials) {
      // try the username/password combo against the getKafkaCluster API call
      // if we get a response, then we can assume the credentials are correct
      const { clientId, secret } = credentials ?? {};
      try {
        if (clientId && secret) {
          const params = new URLSearchParams();
          params.append("client_id", clientId);
          params.append("client_secret", secret);
          params.append("grant_type", "client_credentials");

          log.debug(params);

          const res = await fetch(tokenUrl, {
            cache: "no-cache",
            body: params,
            method: "POST",
            headers: {
              "Content-Type": "application/x-www-form-urlencoded",
            },
          });

          log.debug(res);
          const data = await res.json();
          log.debug(data);

          const accessToken = data.access_token as string;

          if (res.status === 200 && accessToken) {
            return {
              id: "1",
              name: clientId,
              authorization: `Bearer ${accessToken}`,
            };
          }
        }
      } catch {}
      // store the credentials in the session
      // if we didn't get a successful response, the credentials are wrong
      return null;
    },
  });

  return provider;
}
