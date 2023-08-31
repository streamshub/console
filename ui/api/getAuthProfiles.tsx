import { z } from "zod";

const AuthProfileSchema = z.object({
  id: z.string(),
  type: z.string(),
  attributes: z.object({
    name: z.string(),
    bootstrapServers: z.string(),
  }),
});

const Response = z.object({
  data: z.array(AuthProfileSchema)
})

export type AuthProfile = z.infer<typeof AuthProfileSchema>;

export async function getAuthProfiles(): Promise<AuthProfile[]> {
  const url = `${process.env.BACKEND_URL}/api/kafkas?fields%5Bkafkas%5D=name,bootstrapServers,authType`;
  const res = await fetch(url, {
    "headers": {
      "Accept": "application/json",
    },
  });
  const rawData = await res.json();
  return Response.parse(rawData).data;
}
