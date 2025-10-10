import pino from "pino";

export const logger = pino({
  browser: {
    asObject: true,
  },
  level: process.env.LOG_LEVEL ?? "info",
  base: {
    env: process.env.NODE_ENV,
  },
  redact: {
    paths: [
      "access_token",
      "*.access_token",
      "*.*.access_token",
      //
      "refresh_token",
      "*.refresh_token",
      "*.*.refresh_token",
      //
      "id_token",
      "*.id_token",
      "*.*.id_token",
      //
      "iat",
      "*.iat",
      "*.*.iat",
      //
      "exp",
      "*.exp",
      "*.*.exp",
      //
      "expires_at",
      "*.expires_at",
      "*.*.expires_at",
      //
      "client_secret",
      "*.client_secret",
      "*.*.client_secret",
      //
      "clientSecret",
      "*.clientSecret",
      "*.*.clientSecret",
    ],
    censor: (value, path) => {
      if (typeof value === "string") {
        if (path.includes("token")) {
          // return the last chars of the token for cross-reference
          return "***" + value.substring(value.length - 4);
        }
      } else if (typeof value === "number") {
        if (path.includes("expires_at") || path.includes("iat") || path.includes("exp")) {
          // return the last chars of the token for cross-reference
          return value + " => " + new Date(value * 1000).toISOString();
        }
      }
      return "***";
    },
  },
});
