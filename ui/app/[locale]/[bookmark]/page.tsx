import { getTools } from "@/api/tools";
import { getUser } from "@/utils/session";
import { redirect } from "next/navigation";

export default async function bookmarkIndexPage({
  params,
}: {
  params: { bookmark: string };
}) {
  const auth = await getUser();
  const tools = await getTools(auth.username === "admin");

  redirect(`${params.bookmark}${tools[0].url}`);
}
