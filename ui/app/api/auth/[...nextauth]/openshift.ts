import { Provider } from "next-auth/providers/index";

class OpenShiftOAuth {

  provider: Provider = {
    id: "openshift",
    name: process.env.CONSOLE_OAUTH_PROVIDER_NAME,
    type: "oauth",
    authorization: {
      url: process.env.CONSOLE_OAUTH_AUTHORIZATION_URL,
      params: {
        response_type: "code",
        scope: process.env.CONSOLE_OAUTH_AUTHORIZATION_SCOPE
      }
    },
    token: process.env.CONSOLE_OAUTH_TOKEN_URL,
    userinfo: {
      async request(context) {
        return await fetch(process.env.CONSOLE_OAUTH_USERINFO_URL, {
          headers: {
            "Authorization": `Bearer ${context.tokens.access_token ?? ""}`
          }
        })
          .then(res => res.json())
          .then(body => {
            return {
              id: body.metadata.uid,
              sub: body.metadata.name,
              name: body.metadata.name,
              email: "",
              image: ""
            };
          });
      }
    },
    clientId: process.env.CONSOLE_OAUTH_CLIENT_ID,
    clientSecret: process.env.CONSOLE_OAUTH_CLIENT_SECRET,
    profile(profile) {
      return {
        id: profile.sub,
        name: profile.name ?? profile.preferred_username,
        email: profile.email,
        image: profile.image,
      }
    },
  }

  isEnabled() {
    return process.env.CONSOLE_OAUTH_PROVIDER === "openshift"
      && process.env.CONSOLE_OAUTH_AUTHORIZATION_URL
      && process.env.CONSOLE_OAUTH_TOKEN_URL
      && process.env.CONSOLE_OAUTH_USERINFO_URL
      && process.env.CONSOLE_OAUTH_CLIENT_ID
      && process.env.CONSOLE_OAUTH_CLIENT_SECRET;
  }
}

const openshift = new OpenShiftOAuth();
export default openshift;
