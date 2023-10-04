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
  },
  stream,
);
