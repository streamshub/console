"use server";
import { getHeaders } from "@/api/api";
import { ConfigResponseSchema, NodeConfig } from "@/api/nodes/schema";
import { logger } from "@/utils/logger";

const log = logger.child({ module: "api-topics" });

export async function getNodeConfiguration(
  kafkaId: string,
  nodeId: number | string,
): Promise<NodeConfig> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/nodes/${nodeId}/configs`;
  log.debug({ url }, "Fetching node configuration");
  const res = await fetch(url, {
    headers: await getHeaders(),

    next: { tags: [`node-${nodeId}`] },
  });
  const rawData = await res.json();
  log.trace(rawData, "Node configuration response");
  const data = ConfigResponseSchema.parse(rawData);
  return data.data;
}
