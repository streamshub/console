import NextAuth from "next-auth";
import { getAuthOptions } from "./auth-options";
import { logger } from "@/utils/logger";

const handler = async (request: Request) => {
  const authOptions = await getAuthOptions();

  if (!authOptions) {
    return new Response("Authentication not configured", { status: 404 });
  }

  const log = logger.child({ module: "next-auth" });

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

  return authHandler(request);
};

export { handler as GET, handler as POST };
