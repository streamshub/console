import { getUser } from "@/utils/session";
import { z } from "zod";

export async function getHeaders(): Promise<Record<string, string>> {
  const user = await getUser();
  return {
    Accept: "application/json",
    Authorization: `Bearer ${user.accessToken}`,
    "Content-Type": "application/json",
  };
}

export const ApiError = z.object({
  meta: z.object({ type: z.string() }), // z.map(z.string(), z.string()),
  id: z.string().optional(),
  status: z.string().optional(),
  code: z.string().optional(),
  title: z.string(),
  detail: z.string(),
  source: z
    .object({
      pointer: z.string(),
      parameter: z.string(),
      header: z.string(),
    })
    .optional(),
});
