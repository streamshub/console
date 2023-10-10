"use server";
import { getKafkaClusters } from "@/api/kafka";
import {
  ClusterList,
  KafkaResource,
  RegistryResource,
  Resource,
  ResourceTypeKafka,
  ResourceTypeRegistry,
} from "@/api/types";
import { logger } from "@/utils/logger";
import { getSession, setSession } from "@/utils/session";

const log = logger.child({ module: "resources-api" });

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
      return [
        ...(await getKafkaClusters()).map<KafkaResource>((c) => ({
          id: c.id,
          type: "kafka",
          attributes: {
            name: c.attributes.name,
            cluster: c,
            source: "auto",
            bootstrapServer: c.attributes.bootstrapServers,
            principal: "automatic",
            mechanism: "automatic",
          },
        })),
        ...resources.filter((b) => b.type === "kafka"),
      ];
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
  log.debug({ data, session, newSession });
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
  cluster: ClusterList | undefined;
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
      source: "user",
    },
  };
  const newAuthProfiles = [...resources, newProfile];
  await setSession("resources", {
    resources: newAuthProfiles,
    newResource: undefined,
  });
  return newProfile;
}
