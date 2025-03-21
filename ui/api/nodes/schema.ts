import { z } from "zod";

const NodeRoleSchema = z.union([z.literal("broker"), z.literal("controller")]);

const NodePoolsSchema = z.union([
  z.literal("brokers1"),
  z.literal("controllers1"),
  z.literal("dual1"),
]);

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

export const NodeSchema = z.object({
  id: z.string(),
  type: z.literal("nodes"),
  attributes: z.object({
    host: z.string().optional().nullable(),
    port: z.number().optional().nullable(),
    rack: z.string().optional().nullable(),
    nodePool: NodePoolsSchema,
    kafkaVersion: z.string().optional().nullable(),
    roles: z.array(NodeRoleSchema),
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

export type KafkaNode = z.infer<typeof NodeSchema>;

export const NodesListMetaSummary = z.object({
  nodePools: z.record(z.string(), z.array(z.string())),
  statuses: z.record(z.enum(["brokers", "controllers", "combined"]), z.record(z.string(), z.number())),
  leaderId: z.string().optional(),
});

export type NodesListMetaSummary = z.infer<typeof NodesListMetaSummary>;

export const NodesResponseSchema = z.object({
  meta: z.object({
<<<<<<< HEAD
    summary: NodesListMetaSummary,
=======
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
>>>>>>> 1f7997e (feat: UI changes for KRaft)
  }),
  data: z.array(NodeSchema),
});

export type NodeList = z.infer<typeof NodesResponseSchema>;

export type NodeListResponse = z.infer<typeof NodeSchema>;

const ConfigSchema = z.object({
  id: z.string().optional(),
  type: z.string(),
  meta: z.record(z.any()).optional(),
  attributes: z.record(
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

export type NodeConfig = z.infer<typeof ConfigSchema>;

export type BrokerStatus = z.infer<typeof BrokerStatusSchema>;

export type NodePools = z.infer<typeof NodePoolsSchema>;

export type ControllerStatus = z.infer<typeof ControllerStatusSchema>;

export type NodeRoles = z.infer<typeof NodeRoleSchema>;

export const ConfigResponseSchema = z.object({
  data: ConfigSchema,
});
