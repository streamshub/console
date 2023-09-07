import { getSession } from "@/utils/session";
import { redirect } from "next/navigation";
import { CreateBookmarkStep2Page } from "./CreateBookmarkStep2Page.client";

export default async function AsyncCreateBookmarkStep2Page() {
  const session = await getSession();
  const newBookmark = session?.newBookmark;
  const { name, boostrapServer } = newBookmark || {};
  if (!name || !boostrapServer) {
    redirect("/");
  }
  return <CreateBookmarkStep2Page />;
}
