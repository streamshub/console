import { z } from "zod";

const RebalanceStatusSchema = z.union([
  z.literal("New"),
  z.literal("PendingProposal"),
  z.literal("ProposalReady"),
  z.literal("Rebalancing"),
  z.literal("Stopped"),
  z.literal("NotReady"),
  z.literal("Ready"),
  z.literal("ReconciliationPaused"),
]);

const OptimizationResultSchema = z.object({
  numIntraBrokerReplicaMovements: z.number(),
  numReplicaMovements: z.number(),
  onDemandBalancednessScoreAfter: z.number(),
  afterBeforeLoadConfigMap: z.string(),
  intraBrokerDataToMoveMB: z.number(),
  monitoredPartitionsPercentage: z.number(),
  provisionRecommendation: z.string(),
  excludedBrokersForReplicaMove: z.array(z.string()).nullable(),
  excludedBrokersForLeadership: z.array(z.string()).nullable(),
  provisionStatus: z.string(),
  onDemandBalancednessScoreBefore: z.number(),
  recentWindows: z.number(),
  dataToMoveMB: z.number(),
  excludedTopics: z.array(z.string()).nullable(),
  numLeaderMovements: z.number(),
});

export const RebalanceSchema = z.object({
  id: z.string(),
  type: z.literal("kafkaRebalances"),
  meta: z
    .object({
      autoApproval: z.boolean().optional(),
      allowedActions: z.array(z.string()),
    })
    .optional(),
  attributes: z.object({
    name: z.string(),
    namespace: z.string(),
    creationTimestamp: z.string(),
    status: RebalanceStatusSchema,
    mode: z.string().optional(),
    brokers: z.array(z.number()).nullable(),
    sessionId: z.string().nullable(),
    optimizationResult: OptimizationResultSchema,
  }),
});

const RebalancesListSchema = z.object({
  id: z.string(),
  type: z.literal("kafkaRebalances"),
  meta: z.object({
    page: z.object({
      cursor: z.string(),
    }),
    autoApproval: z.boolean(),
    managed: z.boolean().optional(),
    allowedActions: z.array(z.string()),
  }),
  attributes: RebalanceSchema.shape.attributes.pick({
    name: true,
    status: true,
    creationTimestamp: true,
    mode: true,
    brokers: true,
  }),
});

export const RebalanceResponseSchema = z.object({
  meta: z.object({
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
  data: z.array(RebalancesListSchema),
});
export type RebalanceList = z.infer<typeof RebalancesListSchema>;
export type RebalancesResponse = z.infer<typeof RebalanceResponseSchema>;

export type RebalanceResponse = z.infer<typeof RebalanceSchema>;

export type RebalanceStatus = z.infer<typeof RebalanceStatusSchema>;
