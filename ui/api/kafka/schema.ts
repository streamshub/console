import { z } from "zod";

export const NodeSchema = z.object({
  id: z.number(),
  host: z.string(),
  port: z.number(),
  rack: z.string().optional(),
});
export type KafkaNode = z.infer<typeof NodeSchema>;
export const ClusterListSchema = z.object({
  id: z.string(),
  type: z.literal("kafkas"),
  attributes: z.object({
    name: z.string(),
    namespace: z.string(),
    bootstrapServers: z.string().nullable(),
  }),
});
export const ClustersResponseSchema = z.object({
  data: z.array(ClusterListSchema),
});
export type ClusterList = z.infer<typeof ClusterListSchema>;
const ClusterDetailSchema = z.object({
  id: z.string(),
  type: z.literal("kafkas"),
  attributes: z.object({
    name: z.string(),
    namespace: z.string(),
    creationTimestamp: z.string(),
    status: z.string(),
    kafkaVersion: z.string().optional(),
    nodes: z.array(NodeSchema),
    controller: NodeSchema,
    authorizedOperations: z.array(z.string()),
    bootstrapServers: z.string(),
    authType: z.string().optional().nullable(),
    listeners: z
      .array(
        z.object({
          type: z.string(),
          bootstrapServers: z.string().nullable(),
          authType: z.string().nullable(),
        }),
      )
      .optional() /* remove .optional() when `listeners` is added to the fetched fields */,
  }),
});
export const ClusterResponse = z.object({
  data: ClusterDetailSchema,
});
export type ClusterDetail = z.infer<typeof ClusterDetailSchema>;

export const ClusterKpisSchema = z.object({
  broker_state: z.record(z.number()),
  total_topics: z.number(),
  total_partitions: z.number(),
  underreplicated_topics: z.number(),
  replica_count: z.object({
    byNode: z.record(z.number()),
    total: z.number(),
  }),
  leader_count: z.object({
    byNode: z.record(z.number()),
    total: z.number(),
  }),
});
export type ClusterKpis = z.infer<typeof ClusterKpisSchema>;

export const MetricRangeSchema = z.record(z.string(), z.record(z.number()));
export type MetricRange = z.infer<typeof MetricRangeSchema>;
