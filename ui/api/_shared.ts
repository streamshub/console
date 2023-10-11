import { getUser } from "@/utils/session";

export async function getHeaders(): Promise<Record<string, string>> {
  const user = await getUser();
  return {
    Accept: "application/json",
    Authorization: `Bearer ${user.accessToken}`,
  };
}
