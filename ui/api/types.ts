import { z } from "zod";

const ClusterSchema = z.object({
  id: z.string(),
  type: z.string(),
  attributes: z.object({
    name: z.string(),
    bootstrapServers: z.string(),
  }),
});
export const Response = z.object({
  data: z.array(ClusterSchema),
});
export type Cluster = z.infer<typeof ClusterSchema>;

export const ResourceTypeRegistry = "registry" as const;
export const ResourceTypeKafka = "kafka" as const;
export const KafkaResourceSchema = z.object({
  id: z.string(),
  type: z.literal(ResourceTypeKafka),
  attributes: z.object({
    name: z.string(),
    bootstrapServer: z.string(),
    principal: z.string(),
    cluster: ClusterSchema.optional(),
    mechanism: z.string(),
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

const BackendError = z.object({
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

const OffsetSchema = z.object({
  offset: z.number().optional(),
  timestamp: z.string().optional(),
  leaderEpoch: z.number().optional(),
});
const PartitionSchema = z.object({
  partition: z.number(),
  leaderId: z.number(),
  replicas: z.array(
    z.object({
      nodeId: z.number(),
      nodeRack: z.string().optional(),
      inSync: z.boolean(),
      localStorage: BackendError.or(
        z.object({
          size: z.number(),
          offsetLag: z.number(),
          future: z.boolean(),
        }),
      ).optional(),
    }),
  ),
  offsets: z
    .object({
      earliest: OffsetSchema.optional(),
      latest: OffsetSchema.optional(),
      maxTimestamp: OffsetSchema.optional(),
      timestamp: OffsetSchema.optional(),
    })
    .optional()
    .nullable(),
  recordCount: z.number().optional(),
  leaderLocalStorage: z.number().optional(),
});
const ConfigSchema = z.object({
  value: z.string(),
  source: z.string(),
  sensitive: z.boolean(),
  readOnly: z.boolean(),
  type: z.string(),
});
const TopicSchema = z.object({
  id: z.string(),
  type: z.string(),
  attributes: z.object({
    name: z.string(),
    internal: z.boolean(),
    partitions: z.array(PartitionSchema),
    authorizedOperations: z.array(z.string()),
    configs: z.record(z.string(), ConfigSchema),
    recordCount: z.number().optional(),
    totalLeaderLogBytes: z.number().optional(),
  }),
});
export const TopicsResponse = z.object({
  data: z.array(TopicSchema),
});
export const TopicResponse = z.object({
  data: TopicSchema,
});
export type Topic = z.infer<typeof TopicSchema>;
