import { sealData, unsealData } from "iron-session";
import { cookies } from "next/headers";

type Session = {
  principalId?: string;
  topic?: string;
};

export async function getSession() {
  const cookieStore = cookies();
  const encryptedSession = cookieStore.get(process.env.SESSION_COOKIE)?.value;

  return encryptedSession
    ? ((await unsealData(encryptedSession, {
        password: process.env.SESSION_SECRET,
      })) as Session)
    : null;
}

export async function setSession(data: Session) {
  const encryptedSession = await sealData(data, {
    password: process.env.SESSION_SECRET,
  });

  cookies().set({
    name: process.env.SESSION_COOKIE,
    value: encryptedSession,
    httpOnly: true,
  });
}
