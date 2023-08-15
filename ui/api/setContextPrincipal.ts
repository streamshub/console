"use server";

import { getSession, setSession } from "@/utils/session";

export async function setContextPrincipal(principalId: string) {
  const session = await getSession();

  const newSession = {
    ...(session || {}),
    principalId,
    topic: undefined,
  };

  await setSession(newSession);
}

export async function setContextTopic(topic: string) {
  const session = await getSession();

  const newSession = {
    ...(session || {}),
    topic,
  };

  await setSession(newSession);
}
