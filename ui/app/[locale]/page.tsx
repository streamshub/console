import { getSession } from "@/utils/session";
import { redirect } from "next/navigation";

export default async function Home() {
  const { principalId } = (await getSession()) || {};
  return principalId ? redirect("/tools") : redirect("/auth-profiles");
}
