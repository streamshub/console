import { getHeaders } from "@/api/api";
import { logger } from "@/utils/logger";
import { z } from "zod";

const log = logger.child({ module: "kafka-api" });

export const ClusterListSchema = z.object({
  id: z.string(),
  type: z.string(),
  attributes: z.object({
    name: z.string(),
    bootstrapServers: z.string(),
  }),
});
export const ClustersResponse = z.object({
  data: z.array(ClusterListSchema),
});
export type ClusterList = z.infer<typeof ClusterListSchema>;
export const NodeSchema = z.object({
  id: z.number(),
  host: z.string(),
  port: z.number(),
  rack: z.string().optional(),
});
export type KafkaNode = z.infer<typeof NodeSchema>;
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

export async function getKafkaClusters(): Promise<ClusterList[]> {
  const url = `${process.env.BACKEND_URL}/api/kafkas?fields%5Bkafkas%5D=name,bootstrapServers,authType`;
  try {
    log.debug({ url }, "getKafkaClusters");
    const res = await fetch(url, {
      headers: await getHeaders(),
    });
    const rawData = await res.json();
    log.trace({ rawData }, "getKafkaClusters response");
    return ClustersResponse.parse(rawData).data;
  } catch (err) {
    log.error(err, "getKafkaClusters");
    return [];
  }
}

export async function getKafkaCluster(
  clusterId: string,
): Promise<ClusterDetail | null> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${clusterId}/?fields%5Bkafkas%5D=name,namespace,creationTimestamp,nodes,controller,authorizedOperations,bootstrapServers,authType`;
  log.debug({ url }, "getKafkaCluster");
  try {
    const res = await fetch(url, {
      headers: await getHeaders(),
    });
    const rawData = await res.json();
    log.trace({ rawData }, "getKafkaCluster response");
    return ClusterResponse.parse(rawData).data;
  } catch (err) {
    log.error(err, "getKafkaCluster");
    return null;
  }
}
