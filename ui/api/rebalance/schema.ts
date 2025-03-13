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

const ModeSchema = z.union([
  z.literal("full"),
  z.literal("add-brokers"),
  z.literal("remove-brokers"),
]);

const OptimizationResultSchema = z.object({
  numIntraBrokerReplicaMovements: z.number().optional(),
  numReplicaMovements: z.number().optional(),
  onDemandBalancednessScoreAfter: z.number().optional(),
  afterBeforeLoadConfigMap: z.string().optional(),
  intraBrokerDataToMoveMB: z.number().optional(),
  monitoredPartitionsPercentage: z.number().optional(),
  provisionRecommendation: z.string().optional(),
  excludedBrokersForReplicaMove: z.array(z.string()).nullable().optional(),
  excludedBrokersForLeadership: z.array(z.string()).nullable().optional(),
  provisionStatus: z.string().optional(),
  onDemandBalancednessScoreBefore: z.number().optional(),
  recentWindows: z.number().optional(),
  dataToMoveMB: z.number().optional(),
  excludedTopics: z.array(z.string()).nullable().optional(),
  numLeaderMovements: z.number().optional(),
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
    status: RebalanceStatusSchema.nullable(),
    mode: ModeSchema,
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
    optimizationResult: true
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

export type RebalanceMode = z.infer<typeof ModeSchema>;
