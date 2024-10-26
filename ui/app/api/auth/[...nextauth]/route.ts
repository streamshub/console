import NextAuth from "next-auth";
import { NextRequest, NextResponse } from "next/server";
import { getAuthOptions } from "./auth-options";

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
