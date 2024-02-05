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
    kafkaVersion: z.string().optional(),
    bootstrapServers: z.string(),
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
    listeners: z.array(
      z.object({
        name: z.string(),
        type: z.string(),
        bootstrapServers: z.string().nullable(),
        authType: z.string().nullable(),
      }),
    ),
    conditions: z.array(
      z.object({
        type: z.string().optional(),
        status: z.string().optional(),
        reason: z.string().optional(),
        message: z.string().optional(),
        lastTransitionTime: z.string().optional(),
      }),
    ),
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
  volume_stats_capacity_bytes: z.object({
    byNode: z.record(z.number()),
    total: z.number(),
  }),
  volume_stats_used_bytes: z.object({
    byNode: z.record(z.number()),
    total: z.number(),
  }),
});
export type ClusterKpis = z.infer<typeof ClusterKpisSchema>;

export const MetricRangeSchema = z.record(z.string(), z.record(z.number()));
export type MetricRange = z.infer<typeof MetricRangeSchema>;
