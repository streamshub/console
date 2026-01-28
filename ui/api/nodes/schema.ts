import { z } from "zod";

const NodeRoleSchema = z.union([z.literal("broker"), z.literal("controller")]);

const BrokerStatusSchema = z.union([
  z.literal("NotRunning"),
  z.literal("Starting"),
  z.literal("Recovery"),
  z.literal("Running"),
  z.literal("PendingControlledShutdown"),
  z.literal("ShuttingDown"),
  z.literal("Unknown"),
]);

const ControllerStatusSchema = z.union([
  z.literal("QuorumLeader"),
  z.literal("QuorumFollower"),
  z.literal("QuorumFollowerLagged"),
  z.literal("Unknown"),
]);

const NodePoolsSchema = z.record(
  z.string(),
  z.object({
    roles: z.array(z.string()),
    count: z.number(),
  }),
);

export const NodeSchema = z.object({
  id: z.string(),
  type: z.literal("nodes"),
  attributes: z.object({
    host: z.string().optional().nullable(),
    port: z.number().optional().nullable(),
    rack: z.string().optional().nullable(),
    nodePool: z.string().optional().nullable(),
    kafkaVersion: z.string().optional().nullable(),
    roles: z.array(NodeRoleSchema).optional().nullable(),
    metadataState: z
      .object({
        status: z.enum(["leader", "follower", "observer"]),
        logEndOffset: z.number(),
        lag: z.number(),
      })
      .optional()
      .nullable(),
    broker: z
      .object({
        status: BrokerStatusSchema,
        replicaCount: z.number(),
        leaderCount: z.number(),
      })
      .optional()
      .nullable(),
    controller: z
      .object({
        status: ControllerStatusSchema,
      })
      .optional()
      .nullable(),
    storageUsed: z.number().optional().nullable(),
    storageCapacity: z.number().optional().nullable(),
  }),
});

export const StatusesSchema = z.record(
  z.enum(["brokers", "controllers", "combined"]),
  z.record(z.string(), z.number()),
);

export type KafkaNode = z.infer<typeof NodeSchema>;

export type NodePoolsType = z.infer<typeof NodePoolsSchema>;

export const NodesListMetaSummary = z.object({
  nodePools: NodePoolsSchema,
  statuses: StatusesSchema,
  leaderId: z.string().optional(),
});

export type NodesListMetaSummary = z.infer<typeof NodesListMetaSummary>;

export const NodesResponseSchema = z.object({
  meta: z.object({
    summary: NodesListMetaSummary,
    page: z.object({
      total: z.number(),
      pageNumber: z.number().optional(),
    }),
  }),
  links: z.object({
    first: z.string().nullable(),
    prev: z.string().nullable(),
    next: z.string().nullable(),
    last: z.string().nullable(),
  }),
  data: z.array(NodeSchema),
});

export type NodeList = z.infer<typeof NodesResponseSchema>;

export type NodeListResponse = z.infer<typeof NodeSchema>;

const ConfigSchema = z.object({
  id: z.string().optional(),
  type: z.string(),
  meta: z.record(z.string(), z.any()).optional(),
  attributes: z.record(
    z.string(),
    z.object({
      value: z.string().optional(),
      source: z.string().readonly(),
      sensitive: z.boolean().readonly(),
      readOnly: z.boolean().readonly(),
      type: z.string().readonly(),
      documentation: z.string().readonly().optional(),
    }),
  ),
});

export const MetricsSchema = z.object({
  values: z.record(
    z.string(),
    z.array(
      z.object({
        value: z.string(),
        nodeId: z.string(),
      }),
    ),
  ),
  ranges: z.record(
    z.string(),
    z.array(
      z.object({
        range: z.array(z.tuple([z.string(), z.string()])),
        nodeId: z.string().optional(),
      }),
    ),
  ),
});

export const NodeMetricsResponseSchema = z.object({
  data: z.object({
    attributes: z.object({
      metrics: MetricsSchema.optional().nullable(),
    }),
  }),
});

export type NodeConfig = z.infer<typeof ConfigSchema>;

export type BrokerStatus = z.infer<typeof BrokerStatusSchema>;

export type ControllerStatus = z.infer<typeof ControllerStatusSchema>;

export type NodeRoles = z.infer<typeof NodeRoleSchema>;

export type Statuses = z.infer<typeof StatusesSchema>;

export const ConfigResponseSchema = z.object({
  data: ConfigSchema,
});

export type NodeMetrics = z.infer<typeof NodeMetricsResponseSchema>;
