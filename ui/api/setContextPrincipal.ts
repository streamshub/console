"use server";

import { getSession, setSession } from "@/utils/session";

export async function setContextPrincipal(principalId: string) {
  const session = await getSession();

  const newSession = {
    ...(session || {}),
    principalId,
  };

  await setSession(newSession);
}
