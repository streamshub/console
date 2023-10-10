import pino from "pino";

export const logger = pino({
  browser: {
    asObject: true,
  },
  level: "trace",
  base: {
    env: process.env.NODE_ENV,
  },
  redact: {
    paths: [
      "*.access_token",
      "*.refresh_token",
      "*.id_token",
      "access_token",
      "refresh_token",
      "id_token",
      "iat",
      "*.iat",
      "exp",
      "*.exp",
      "expires_at",
      "*.expires_at",
    ],
    censor: (value, path) => {
      const p = path.join("");
      if (p.includes("token") && typeof value === "string") {
        // return the last chars of the token for cross-reference
        return "***" + value.substring(value.length - 4);
      } else if (
        (p.includes("expires_at") || p.includes("iat") || p.includes("exp")) &&
        typeof value === "number"
      ) {
        // return the last chars of the token for cross-reference
        return new Date(value * 1000).toLocaleString();
      }
      return "***";
    },
  },
});
