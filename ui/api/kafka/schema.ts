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
  type: z.string(),
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
  type: z.string(),
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
