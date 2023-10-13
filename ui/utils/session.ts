"use server";
import { Resources, ResourcesSchema } from "@/api/resources";
import { authOptions } from "@/app/api/auth/[...nextauth]/route";
import { sealData, unsealData } from "iron-session";
import { getServerSession } from "next-auth";
import { cookies } from "next/headers";

export async function getSession(scope: "resources") {
  const user = await getUser();
  const cookieStore = cookies();
  const encryptedSession = cookieStore.get(`${user.username}:${scope}`)?.value;

  try {
    const rawSession = encryptedSession
      ? await unsealData(encryptedSession, {
          password: process.env.SESSION_SECRET,
        })
      : null;
    switch (scope) {
      case "resources":
        return ResourcesSchema.parse(rawSession);
    }
  } catch {
    switch (scope) {
      case "resources":
        return { resources: [], newResource: {} };
    }
  }
}

export async function setSession(
  scope: "kafka" | "resources",
  session: Resources,
): Promise<Resources> {
  const user = await getUser();
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
