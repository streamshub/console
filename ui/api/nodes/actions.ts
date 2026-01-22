"use server";

import {
  fetchData,
  ApiResponse,
  filterEq,
  filterIn,
  filterLike,
  sortParam,
} from "@/api/api";
import {
  NodesResponseSchema,
  NodeList,
  ConfigResponseSchema,
  NodeConfig,
  NodeRoles,
  BrokerStatus,
  ControllerStatus,
  NodeMetricsResponseSchema,
  NodeMetrics,
} from "@/api/nodes/schema";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";

export async function getNodes(
  kafkaId: string,
  params?: {
    fields?: string;
    brokerStatus?: BrokerStatus[];
    controllerStatus?: ControllerStatus[];
    nodePool?: string[];
    roles?: NodeRoles[];
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
      "filter[broker.status]": filterIn(params?.brokerStatus),
      "filter[controller.status]": filterIn(params?.controllerStatus),
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

  return fetchData(`/api/kafkas/${kafkaId}/nodes`, sp, (rawData: any) =>
    NodesResponseSchema.parse(rawData),
  );
}

export async function getNodeConfiguration(
  kafkaId: string,
  nodeId: number | string,
): Promise<ApiResponse<NodeConfig>> {
  return fetchData(
    `/api/kafkas/${kafkaId}/nodes/${nodeId}/configs`,
    "",
    (rawData) => ConfigResponseSchema.parse(rawData).data,
  );
}

export async function getNodeMetrics(
  kafkaId: string,
  nodeId: number | string,
  duration: number,
): Promise<ApiResponse<NodeMetrics>> {
  return fetchData(
    `/api/kafkas/${kafkaId}/nodes/${nodeId}/metrics?duration=${duration}`,
    "",
    (rawData) => NodeMetricsResponseSchema.parse(rawData),
  );
}
