"use server";
import { KafkaResourceSchema, ResourceSchema } from "@/api/types";
import { authOptions } from "@/app/api/auth/[...nextauth]/route";
import { sealData, unsealData } from "iron-session";
import { getServerSession } from "next-auth";
import { cookies } from "next/headers";
import z from "zod";

const ResourcesSchema = z.object({
  newResource: z
    .object({
      name: z.string().optional(),
      boostrapServer: z.string().optional(),
      principal: z.string().optional(),
    })
    .optional(),
  resources: z.array(ResourceSchema),
});
type Resources = z.infer<typeof ResourcesSchema>;

const KafkaSessionSchema = z.object({
  lastUsed: KafkaResourceSchema.optional(),
});
type KafkaSession = z.infer<typeof KafkaSessionSchema>;

export async function getSession(scope: "resources"): Promise<Resources>;
export async function getSession(scope: "kafka"): Promise<KafkaSession>;
export async function getSession(scope: "kafka" | "resources") {
  const user = await getUser();
  const cookieStore = cookies();
  const encryptedSession = cookieStore.get(`${user.username}:${scope}`)?.value;

  try {
    const rawSession = encryptedSession
      ? await unsealData(encryptedSession, {
          password: process.env.SESSION_SECRET,
        })
      : null;
    switch (scope) {
      case "resources":
        return ResourcesSchema.parse(rawSession);
      case "kafka":
        return KafkaSessionSchema.parse(rawSession);
    }
  } catch {
    switch (scope) {
      case "resources":
        return { resources: [], newResource: {} };
      case "kafka":
        return {};
    }
  }
}
export async function setSession(
  scope: "kafka",
  session: KafkaSession,
): Promise<KafkaSession>;
export async function setSession(
  scope: "resources",
  session: Resources,
): Promise<Resources>;
export async function setSession(
  scope: "kafka" | "resources",
  session: KafkaSession | Resources,
): Promise<KafkaSession | Resources> {
  const user = await getUser();
  const encryptedSession = await sealData(session, {
    password: process.env.SESSION_SECRET,
  });

  cookies().set({
    name: `${user.username}:${scope}`,
    value: encryptedSession,
    httpOnly: true,
  });
  return session;
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
