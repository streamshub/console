"use server";
import { getSession, setSession } from "@/utils/session";
import { redirect } from "next/navigation";
import { z } from "zod";

const ClusterSchema = z.object({
  id: z.string(),
  type: z.string(),
  attributes: z.object({
    name: z.string(),
    bootstrapServers: z.string(),
  }),
});

const Response = z.object({
  data: z.array(ClusterSchema),
});

export type Cluster = z.infer<typeof ClusterSchema>;

const AuthProfileSchema = z.object({
  id: z.string(),
  type: z.string(),
  attributes: z.object({
    name: z.string(),
    cluster: ClusterSchema,
    mechanism: z.string(),
  }),
});

export type AuthProfile = z.infer<typeof AuthProfileSchema>;

export async function getAuthProfiles(): Promise<AuthProfile[]> {
  const session = await getSession();

  if (session?.authProfiles && Array.isArray(session.authProfiles)) {
    return session.authProfiles.map((p) => AuthProfileSchema.parse(p));
  }
  return [];
}

export async function getAuthProfile(id: string) {
  const authProfiles = await getAuthProfiles();

  const authProfile = authProfiles.find((p) => p.id === id);
  if (!authProfile) {
    redirect("/");
  }
  return authProfile;
}

export async function createAuthProfile(clusterId: string, name: string) {
  const session = await getSession();
  const clusters = await getClusters();
  const cluster = clusters.find((c) => c.id === clusterId);
  if (!cluster) throw new Error("Invalid cluster id");
  const authProfiles = (session?.authProfiles || []) as AuthProfile[];
  const newProfile: AuthProfile = {
    id: `${authProfiles.length + 1}`,
    type: "auth_profile",
    attributes: {
      cluster,
      mechanism: "PLAIN",
      name,
    },
  };
  const newAuthProfiles = [...authProfiles, newProfile];
  await setSession({ authProfiles: newAuthProfiles });
  return newProfile;
}

export async function getClusters(): Promise<Cluster[]> {
  const url = `${process.env.BACKEND_URL}/api/kafkas?fields%5Bkafkas%5D=name,bootstrapServers,authType`;
  const res = await fetch(url, {
    headers: {
      Accept: "application/json",
    },
  });
  const rawData = await res.json();
  return Response.parse(rawData).data;
}
