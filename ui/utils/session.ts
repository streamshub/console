import { authOptions } from "@/app/api/auth/[...nextauth]/route";
import { sealData, unsealData } from "iron-session";
import { getServerSession } from "next-auth";
import { cookies } from "next/headers";
import { z } from 'zod';

const SessionData = z.object({
  principalId: z.string().optional(),
  topic: z.string().optional()
})

type Session = {
  principalId?: string;
  topic?: string;
};

export async function getSession() {
  const cookieStore = cookies();
  const encryptedSession = cookieStore.get(process.env.SESSION_COOKIE)?.value;

  try {
    const session = SessionData.parse(encryptedSession
      ? ((await unsealData(encryptedSession, {
        password: process.env.SESSION_SECRET,
      })))
      : null);
    return session;
  } catch {
    return null;
  }

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

export async function getUser() {

  const auth = await getServerSession(authOptions)
  if (!auth || !auth.user) {
    throw Error('Unauthorized')
  }

  return {
    username: auth.user.name || auth.user.email || 'User'
  };
}
