"use server";

import { fetchData, ApiResponse, filterEq, filterIn, filterLike, sortParam } from "@/api/api";
import { NodesResponseSchema, NodeList, ConfigResponseSchema, NodeConfig } from "@/api/nodes/schema";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";

export async function getNodes(
  kafkaId: string,
  params?: {
    fields?: string;
    status?: string[];
    nodePool?: string[];
    roles?: string[];
    id?: string;
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
  },
): Promise<ApiResponse<NodeList>> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[nodes]": params?.fields,
      "filter[status]": filterIn(params?.status),
      "filter[nodePool]": filterIn(params?.nodePool),
      "filter[roles]": filterIn(params?.roles),
      "page[size]": params?.pageSize,
      "page[after]": params?.pageCursor?.startsWith("after:")
        ? params?.pageCursor.slice(6)
        : undefined,
      "page[before]": params?.pageCursor?.startsWith("before:")
        ? params?.pageCursor.slice(7)
        : undefined,
      sort: sortParam(params?.sort, params?.sortDir),
    }),
  );

  return fetchData(
    `/api/kafkas/${kafkaId}/nodes`,
    sp,
    (rawData: any) => NodesResponseSchema.parse(rawData),
  );
}

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
