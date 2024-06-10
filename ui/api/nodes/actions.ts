"use server";

import { fetchData, ApiResponse } from "@/api/api";
import { ConfigResponseSchema, NodeConfig } from "@/api/nodes/schema";

export async function getNodeConfiguration(
  kafkaId: string,
  nodeId: number | string,
): Promise<ApiResponse<NodeConfig>> {
  return fetchData(
    `/api/kafkas/${kafkaId}/nodes/${nodeId}/configs`,
    "",
    (rawData) => ConfigResponseSchema.parse(rawData).data
  );
}
