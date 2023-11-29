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
    nodes: z.array(NodeSchema),
    controller: NodeSchema,
    authorizedOperations: z.array(z.string()),
    bootstrapServers: z.string(),
    authType: z.string().optional().nullable(),
    listeners: z.array(
      z.object({
        type: z.string(),
        bootstrapServers: z.string().nullable(),
        authType: z.string().nullable(),
      })
    ).optional() /* remove .optional() when `listeners` is added to the fetched fields */,
    metrics: z.object({
      values: z.record(
        z.array(
          z.object({
            value: z.string(),
            nodeId: z.string().optional(),
          })
        )
      ),
      ranges: z.record(
        z.array(
          z.object({
            range: z.array(z.array(z.string())),
            nodeId: z.string().optional(),
          })
        )
      ),
    }).optional(),
  }),
});
export const ClusterResponse = z.object({
  data: ClusterDetailSchema,
});
export type ClusterDetail = z.infer<typeof ClusterDetailSchema>;

const ValueMetricSchema = z.object({
  value: z.string(),
});
const ValueMetricWithNodeIdSchema = z.object({
  value: z.string(),
  nodeId: z.string(),
});
const RangeEntrySchema = z.tuple([z.string(), z.string()]);
const RangeMetricSchema = z.object({
  range: z.array(RangeEntrySchema),
});
const RangeMetricWithNodeIdSchema = z.object({
  range: z.array(RangeEntrySchema),
  nodeId: z.string(),
});

export const ClusterMetricsSchema = z.object({
  data: z.object({
    id: z.string(),
    type: z.literal("kafkas"),
    attributes: z.object({
      metrics: z.object({
        values: z.strictObject({
          broker_state: z.array(ValueMetricWithNodeIdSchema),
          total_topics: z.array(ValueMetricSchema),
          underreplicated_topics: z.array(ValueMetricSchema),
          total_partitions: z.array(ValueMetricSchema),
        }),
        ranges: z.strictObject({
          volume_stats_used_bytes: z.array(RangeMetricWithNodeIdSchema),
          outgoing_byte_rate: z.array(RangeMetricSchema),
          memory_usage_bytes: z.array(RangeMetricWithNodeIdSchema),
          incoming_byte_rate: z.array(RangeMetricSchema),
          cpu_usage_seconds: z.array(RangeMetricWithNodeIdSchema),
          volume_stats_capacity_bytes: z.array(RangeMetricWithNodeIdSchema),
        }),
      }),
    }),
  }),
});
export type ClusterMetrics = z.infer<typeof ClusterMetricsSchema>;
