import { z } from "zod";

export const NodeSchema = z.object({
  id: z.string(),
  type: z.literal("nodes"),
  attributes: z.object({
    host: z.string().optional().nullable(),
    port: z.number().optional().nullable(),
    rack: z.string().optional().nullable(),
    nodePool: z.string().optional().nullable(),
    kafkaVersion: z.string().optional().nullable(),
    roles: z.array(z.enum([ "controller", "broker" ])).optional(),
    metadataState: z.object({
      status: z.enum([ "leader", "follower", "observer" ]),
      logEndOffset: z.number(),
      lastFetchTimestamp: z.string().nullable(),
      lastCaughtUpTimestamp: z.string().nullable(),
      lag: z.number(),
      timeLag: z.number(),
    }).optional().nullable(),
    broker: z.object({
      status: z.enum([
        "NotRunning",
        "Starting",
        "Recovery",
        "Running",
        "PendingControlledShutdown",
        "ShuttingDown",
        "Unknown"
      ]),
      replicaCount: z.number(),
      leaderCount: z.number(),
    }).optional().nullable(),
    controller: z.object({
      status: z.enum([
        "QuorumLeader",
        "QuorumFollower",
        "QuorumFollowerLagged",
        "Unknown"
      ]),
    }).optional().nullable(),
    storageUsed: z.number().optional().nullable(),
    storageCapacity: z.number().optional().nullable(),
  }),
});

export type KafkaNode = z.infer<typeof NodeSchema>;

export const NodesListMetaSummary = z.object({
  nodePools: z.record(z.string(), z.array(z.string())),
  statuses: z.record(z.string(), z.record(z.string(), z.number())),
  totalNodes: z.number(),
});

export type NodesListMetaSummary = z.infer<typeof NodesListMetaSummary>;

export const NodesResponseSchema = z.object({
  meta: z.object({
    summary: NodesListMetaSummary,
  }),
  data: z.array(NodeSchema),
});

export type NodeList = z.infer<typeof NodesResponseSchema>;

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

export const ConfigResponseSchema = z.object({
  data: ConfigSchema,
});
