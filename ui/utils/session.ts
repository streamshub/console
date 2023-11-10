"use server";
import { authOptions } from "@/app/api/auth/[...nextauth]/route";
import { sealData, unsealData } from "iron-session";
import { getServerSession } from "next-auth";
import { cookies } from "next/headers";

export async function getSession<T extends Record<string, unknown>>(
  scope: string,
) {
  const user = await getUser();
  if (!user.username) {
    throw new Error("Can't get session for unauthenticated users");
  }
  const cookieStore = cookies();
  const encryptedSession = cookieStore.get(`${user.username}:${scope}`)?.value;

  if (!encryptedSession) {
    return {} as T;
  }
  try {
    const rawSession = await unsealData(encryptedSession, {
      password: process.env.SESSION_SECRET,
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
  if (!user.username) {
    throw new Error("Can't set session for unauthenticated users");
  }
  const encryptedSession = await sealData(session, {
    password: process.env.SESSION_SECRET,
  });

  cookies().set({
    name: `${user.username}:${scope}`,
    value: encryptedSession,
    httpOnly: true,
  });
  return session;
}

export async function getUser() {
  const auth = await getServerSession(authOptions);

  return {
    username: auth?.user?.name || auth?.user?.email,
    accessToken: auth?.accessToken,
  };
}
