import { getKafkaClusters } from "@/api/kafka";
import { logger } from "@/utils/logger";
import { getSession, setSession } from "@/utils/session";
import z from "zod";

const log = logger.child({ module: "resources-api" });

export const ResourceTypeRegistry = "registry" as const;
export const ResourceTypeKafka = "kafka" as const;
export const KafkaResourceSchema = z.object({
  id: z.string(),
  type: z.literal(ResourceTypeKafka),
  attributes: z.object({
    name: z.string(),
    bootstrapServer: z.string(),
    principal: z.string(),
    clusterId: z.string().optional(),
    mechanism: z.string(),
    source: z.union([z.literal("user"), z.literal("auto")]),
  }),
});
export type KafkaResource = z.infer<typeof KafkaResourceSchema>;
export const RegistryResourceSchema = z.object({
  id: z.string(),
  type: z.literal(ResourceTypeRegistry),
  attributes: z.object({
    name: z.string(),
    url: z.string(),
  }),
});
export type RegistryResource = z.infer<typeof RegistryResourceSchema>;
export const ResourceSchema = z.discriminatedUnion("type", [
  KafkaResourceSchema,
  RegistryResourceSchema,
]);
export type Resource = z.infer<typeof ResourceSchema>;
type ResourceType = typeof ResourceTypeKafka | typeof ResourceTypeRegistry;

export const ResourcesSchema = z.object({
  newResource: z
    .object({
      name: z.string().optional(),
      boostrapServer: z.string().optional(),
      principal: z.string().optional(),
    })
    .optional(),
  resources: z.array(ResourceSchema),
});
export type Resources = z.infer<typeof ResourcesSchema>;

export async function getResources(
  scope: typeof ResourceTypeRegistry,
): Promise<RegistryResource[]>;
export async function getResources(
  scope: typeof ResourceTypeKafka,
): Promise<KafkaResource[]>;
export async function getResources(scope: unknown): Promise<unknown> {
  "use server";
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
  "use server";
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
  clusterId,
}: {
  bootstrapServer: string;
  principal: string;
  name: string;
  clusterId: string | undefined;
}) {
  const session = await getSession("resources");
  const resources = (session?.resources || []) as Resource[];
  const newProfile: KafkaResource = {
    id: crypto.randomUUID(),
    type: ResourceTypeKafka,
    attributes: {
      clusterId,
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
