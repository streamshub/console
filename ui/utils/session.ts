"use server";

import { getAuthOptions } from "@/app/api/auth/[...nextauth]/auth-options";
import { logger } from "@/utils/logger";
import { sealData, unsealData } from "iron-session";
import { getServerSession } from "next-auth";
import { cookies } from "next/headers";

const log = logger.child({ module: "session" });

export async function getSession<T extends Record<string, unknown>>(
  scope: string,
) {
  const user = await getUser();
  const username = user.username ?? "anonymous";
  const cookieStore = await cookies();
  const encryptedSession = cookieStore.get(`${username}:${scope}`)?.value;

  if (!encryptedSession) {
    return {} as T;
  }
  try {
    const rawSession = await unsealData(encryptedSession, {
      password: process.env.NEXTAUTH_SECRET,
    });
    return rawSession as T;
  } catch {
    return {} as T;
  }
}

export async function setSession<T extends Record<string, unknown>>(
  scope: string,
  session: T,
) {
  const user = await getUser();
  const username = user.username ?? "anonymous";
  const encryptedSession = await sealData(session, {
    password: process.env.NEXTAUTH_SECRET,
  });

  (await cookies()).set({
    name: `${username}:${scope}`,
    value: encryptedSession,
    httpOnly: true,
  });
  return session;
}

export async function getUser() {
  log.trace("About to getServerSession");
  const authOptions = await getAuthOptions();
  const auth = await getServerSession(authOptions);

  return {
    username: auth?.user?.name || auth?.user?.email,
    authorization: auth?.authorization,
  };
}
