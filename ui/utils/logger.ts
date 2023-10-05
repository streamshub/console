import pino from "pino";
import pretty from "pino-pretty";

const stream = pretty({
  colorize: true,
});

export const logger = pino(
  {
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
        "expires_at",
        "*.expires_at",
      ],
      censor: (value, path) => {
        if (path.join("").includes("token") && typeof value === "string") {
          // return the last chars of the token for cross-reference
          return "***" + value.substring(value.length - 4);
        } else if (
          path.join("").includes("expires_at") &&
          typeof value === "number"
        ) {
          // return the last chars of the token for cross-reference
          return new Date(value * 1000).toLocaleString();
        }
        return "***";
      },
    },
  },
  stream,
);
