import { getTools } from "@/api/tools";
import { getUser } from "@/utils/session";
import { redirect } from "next/navigation";

export default async function AuthProfileIndexPage({
  params,
}: {
  params: { authProfile: string };
}) {
  const auth = await getUser();
  const tools = await getTools(auth.username === "admin");

  redirect(`${params.authProfile}${tools[0].url}`);
}
