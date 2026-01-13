import { z } from "zod";
import { NodesListMetaSummary } from "../nodes/schema";

export const KafkaClusterKindSchema = z
  .enum(["kafkas.kafka.strimzi.io", "virtualkafkaclusters.kroxylicious.io"])
  .nullable()
  .optional();

export const ClusterListSchema = z.object({
  id: z.string(),
  type: z.literal("kafkas"),
  meta: z.object({
    configured: z.boolean(),
    authentication: z
      .union([
        z.object({
          method: z.literal("anonymous"),
        }),
        z.object({
          method: z.literal("basic"),
        }),
        z.object({
          method: z.literal("oauth"),
          tokenUrl: z.string().nullable().optional(),
        }),
      ])
      .nullable()
      .optional(),
  }),
  attributes: z.object({
    name: z.string(),
    namespace: z.string().nullable().optional(),
    kafkaVersion: z.string().nullable().optional(),
  }),
});

export const ClustersResponseSchema = z.object({
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
  data: z.array(ClusterListSchema),
});

export type CLustersResponse = z.infer<typeof ClustersResponseSchema>;

export type ClusterList = z.infer<typeof ClusterListSchema>;

const ClusterDetailSchema = z.object({
  id: z.string(),
  type: z.literal("kafkas"),
  meta: z
    .object({
      reconciliationPaused: z.boolean().optional(),
      kind: KafkaClusterKindSchema,
      managed: z.boolean(),
      privileges: z.array(z.string()).optional(),
    })
    .optional(),
  attributes: z.object({
    name: z.string(),
    namespace: z.string().nullable().optional(),
    creationTimestamp: z.string().nullable().optional(),
    status: z.string().nullable().optional(),
    kafkaVersion: z.string().nullable().optional(),
    authorizedOperations: z.array(z.string()).optional(),
    cruiseControlEnabled: z.boolean().optional(),
    listeners: z
      .array(
        z.object({
          type: z.string(),
          bootstrapServers: z.string().nullable(),
          authType: z.string().nullable(),
        }),
      )
      .nullable()
      .optional(),
    conditions: z
      .array(
        z.object({
          type: z.string().optional(),
          status: z.string().optional(),
          reason: z.string().optional(),
          message: z.string().optional(),
          lastTransitionTime: z.string().optional(),
        }),
      )
      .nullable()
      .optional(),
    nodePools: z.array(z.string()).optional().nullable(),
    metrics: z
      .object({
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
      })
      .optional()
      .nullable(),
  }),
  relationships: z.object({
    nodes: z
      .object({
        meta: z.object({
          summary: NodesListMetaSummary,
        }),
        data: z.array(z.any()),
      })
      .optional(),
  }),
});

export const ClusterResponse = z.object({
  data: ClusterDetailSchema,
});
export type ClusterDetail = z.infer<typeof ClusterDetailSchema>;
