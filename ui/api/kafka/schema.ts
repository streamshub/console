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
    bootstrapServers: z.string(),
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
  }),
});
export const ClusterResponse = z.object({
  data: ClusterDetailSchema,
});
export type ClusterDetail = z.infer<typeof ClusterDetailSchema>;
