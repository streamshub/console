import { BookmarkSchema } from "@/api/types";
import { authOptions } from "@/app/api/auth/[...nextauth]/route";
import { sealData, unsealData } from "iron-session";
import { getServerSession } from "next-auth";
import { cookies } from "next/headers";
import z from "zod";

const SessionShape = z.object({
  newBookmark: z
    .object({
      name: z.string().optional(),
      boostrapServer: z.string().optional(),
      principal: z.string().optional(),
    })
    .optional(),
  bookmarks: z.array(BookmarkSchema).optional(),
});

export async function getSession() {
  const user = await getUser();
  const cookieStore = cookies();
  const encryptedSession = cookieStore.get(user.username)?.value;

  try {
    const rawSession = encryptedSession
      ? await unsealData(encryptedSession, {
          password: process.env.SESSION_SECRET,
        })
      : null;
    return SessionShape.parse(rawSession);
  } catch {
    return null;
  }
}

export async function setSession(data: any) {
  const user = await getUser();
  const encryptedSession = await sealData(data, {
    password: process.env.SESSION_SECRET,
  });

  cookies().set({
    name: user.username,
    value: encryptedSession,
    httpOnly: true,
  });
}

export async function getUser() {
  const auth = await getServerSession(authOptions);
  if (!auth || !auth.user) {
    throw Error("Unauthorized");
  }

  return {
    username: auth.user.name || auth.user.email || "User",
  };
}
