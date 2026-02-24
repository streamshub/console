"use server";
import {
  fetchData,
  patchData,
  sortParam,
  ApiResponse,
  filterIn,
  filterLike,
} from "@/api/api";
import {
  ConsumerGroup,
  ConsumerGroupResponseSchema,
  ConsumerGroupsResponse,
  ConsumerGroupsResponseSchema,
  ConsumerGroupState,
  GroupType,
} from "@/api/groups/schema";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";

export async function getConsumerGroup(
  kafkaId: string,
  groupId: string,
): Promise<ApiResponse<ConsumerGroup>> {
  return fetchData(
    `/api/kafkas/${kafkaId}/groups/${groupId}`,
    "",
    (rawData) => ConsumerGroupResponseSchema.parse(rawData).data,
  );
}

export async function getConsumerGroups(
  kafkaId: string,
  params: {
    fields?: string;
    id?: string;
    type?: GroupType[];
    consumerGroupState?: ConsumerGroupState[];
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
  },
): Promise<ApiResponse<ConsumerGroupsResponse>> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[groups]":
        params.fields ?? "groupId,type,protocol,state,simpleConsumerGroup,members,offsets",
      "filter[id]": filterLike(params.id),
      "filter[type]": filterIn(params.type),
      "filter[state]": filterIn(params.consumerGroupState),
      "page[size]": params.pageSize,
      "page[after]": params.pageCursor?.startsWith("after:")
        ? params.pageCursor.slice(6)
        : undefined,
      "page[before]": params.pageCursor?.startsWith("before:")
        ? params.pageCursor.slice(7)
        : undefined,
      sort: sortParam(params.sort, params.sortDir),
    }),
  );

  return fetchData(`/api/kafkas/${kafkaId}/groups`, sp, (rawData) =>
    ConsumerGroupsResponseSchema.parse(rawData),
  );
}

export async function getTopicConsumerGroups(
  kafkaId: string,
  topicId: string,
  params: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
  },
): Promise<ApiResponse<ConsumerGroupsResponse>> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[groups]":
        "groupId,type,protocol,state,simpleConsumerGroup,members,offsets,coordinator,partitionAssignor",
      "page[size]": params.pageSize,
      "page[after]": params.pageCursor,
      sort: sortParam(params.sort, params.sortDir),
    }),
  );
  return fetchData(
    `/api/kafkas/${kafkaId}/topics/${topicId}/groups`,
    sp,
    (rawData) => ConsumerGroupsResponseSchema.parse(rawData),
  );
}

export async function updateConsumerGroup(
  kafkaId: string,
  consumerGroupId: string,
  offsets: Array<{
    topicId: string;
    partition?: number;
    offset: string | number | null;
    metadata?: string;
  }>,
  dryRun?: boolean,
): Promise<ApiResponse<ConsumerGroup | undefined>> {
  return patchData(
    `/api/kafkas/${kafkaId}/groups/${consumerGroupId}`,
    {
      meta: {
        dryRun: dryRun,
      },
      data: {
        type: "groups",
        id: consumerGroupId,
        attributes: {
          offsets,
        },
      },
    },
    (rawData) =>
      dryRun ? ConsumerGroupResponseSchema.parse(rawData).data : undefined,
  );
}
