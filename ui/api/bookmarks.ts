"use server";
import { Bookmark, BookmarkSchema, Cluster, Response } from "@/api/types";
import { getSession, setSession } from "@/utils/session";
import { redirect } from "next/navigation";

export async function getBookmarks(): Promise<Bookmark[]> {
  const session = await getSession();

  if (session?.bookmarks && Array.isArray(session.bookmarks)) {
    return session.bookmarks.map((p) => BookmarkSchema.parse(p));
  }
  return [];
}

export async function getBookmark(id: string) {
  const bookmarks = await getBookmarks();

  const bookmark = bookmarks.find((p) => p.id === id);
  if (!bookmark) {
    redirect("/");
  }
  return bookmark;
}

export async function setPartialBookmark(formData: FormData) {
  const session = await getSession();
  const newBookmark = session?.newBookmark || {};
  const data = Object.fromEntries(formData);
  const newSession = { ...session, newBookmark: { ...newBookmark, ...data } };
  console.dir({ data, session, newSession });
  await setSession(newSession);
}

export async function createBookmark({
  name,
  bootstrapServer,
  principal,
  cluster,
}: {
  bootstrapServer: string;
  principal: string;
  name: string;
  cluster: Cluster | undefined;
}) {
  const session = await getSession();
  const bookmarks = (session?.bookmarks || []) as Bookmark[];
  const newProfile: Bookmark = {
    id: crypto.randomUUID(),
    type: "bookmark",
    attributes: {
      cluster,
      mechanism: "PLAIN",
      name,
      bootstrapServer,
      principal,
    },
  };
  const newAuthProfiles = [...bookmarks, newProfile];
  await setSession({ bookmarks: newAuthProfiles, newBookmark: undefined });
  return newProfile;
}

export async function getClusters(): Promise<Cluster[]> {
  const url = `${process.env.BACKEND_URL}/api/kafkas?fields%5Bkafkas%5D=name,bootstrapServers,authType`;
  try {
    const res = await fetch(url, {
      headers: {
        Accept: "application/json",
      },
    });
    const rawData = await res.json();
    return Response.parse(rawData).data;
  } catch (err) {
    console.error(err);
    throw err;
  }
}
