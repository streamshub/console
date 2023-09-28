"use server";
import {
  Cluster,
  KafkaResource,
  RegistryResource,
  Resource,
  ResourceTypeKafka,
  ResourceTypeRegistry,
  Response,
} from "@/api/types";
import { getSession, setSession } from "@/utils/session";

type ResourceType = typeof ResourceTypeKafka | typeof ResourceTypeRegistry;

export async function getResources(
  scope: typeof ResourceTypeRegistry,
): Promise<RegistryResource[]>;
export async function getResources(
  scope: typeof ResourceTypeKafka,
): Promise<KafkaResource[]>;
export async function getResources(scope: unknown): Promise<unknown> {
  const { resources } = await getSession("resources");
  switch (scope) {
    case "kafka":
      return resources.filter((b) => b.type === "kafka");
    case "registry":
      return resources.filter((b) => b.type === "registry");
  }
}

export async function getResource(
  id: string,
  scope: typeof ResourceTypeRegistry,
): Promise<RegistryResource | undefined>;
export async function getResource(
  id: string,
  scope: typeof ResourceTypeKafka,
): Promise<KafkaResource | undefined>;
export async function getResource(
  id: string,
  scope: unknown,
): Promise<unknown> {
  let resource;
  if (typeof scope === typeof ResourceTypeKafka) {
    const resources = await getResources("kafka");
    resource = resources.find((p) => p.id === id);
  } else if (typeof scope === typeof ResourceTypeRegistry) {
    const resources = await getResources("registry");
    resource = resources.find((p) => p.id === id);
  }
  return resource;
}

export async function setPartialResource(formData: FormData) {
  const session = await getSession("resources");
  const newResource = session?.newResource || {};
  const data = Object.fromEntries(formData);
  const newSession = { ...session, newResource: { ...newResource, ...data } };
  console.dir({ data, session, newSession });
  await setSession("resources", newSession);
}

export async function createKafkaResource({
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
  const session = await getSession("resources");
  const resources = (session?.resources || []) as Resource[];
  const newProfile: KafkaResource = {
    id: crypto.randomUUID(),
    type: ResourceTypeKafka,
    attributes: {
      cluster,
      mechanism: "PLAIN",
      name,
      bootstrapServer,
      principal,
    },
  };
  const newAuthProfiles = [...resources, newProfile];
  await setSession("resources", {
    resources: newAuthProfiles,
    newResource: undefined,
  });
  return newProfile;
}

export async function getClusters(): Promise<Cluster[]> {
  const url = `${process.env.BACKEND_URL}/api/kafkas?fields%5Bkafkas%5D=name,bootstrapServers,authType`;
  try {
    const res = await fetch(url, {
      headers: {
        Accept: "application/json",
      },
      cache: "no-store",
    });
    const rawData = await res.json();
    return Response.parse(rawData).data;
  } catch (err) {
    console.error(err);
    throw err;
  }
}
