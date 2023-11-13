"use server";
import { getHeaders } from "@/api/api";
import {
  ConsumerGroup,
  ConsumerGroupResponseSchema,
  ConsumerGroupsResponse,
  ConsumerGroupsResponseSchema,
} from "@/api/consumerGroups/schema";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { logger } from "@/utils/logger";

const log = logger.child({ module: "topics-api" });

export async function getConsumerGroup(
  kafkaId: string,
  groupId: string,
): Promise<ConsumerGroup> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[consumerGroups]":
        "state,simpleConsumerGroup,members,offsets,authorizedOperations,coordinator,partitionAssignor",
    }),
  );
  const cgQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/consumerGroups/${groupId}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
    next: {
      tags: [`consumer-group-${kafkaId}-${groupId}`],
    },
  });
  log.debug({ url }, "getConsumerGroup");
  const rawData = await res.json();
  log.debug({ url, rawData }, "getConsumerGroup response");
  return ConsumerGroupResponseSchema.parse(rawData).data;
}

export async function getConsumerGroups(
  kafkaId: string,
  params: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
  },
): Promise<ConsumerGroupsResponse> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[consumerGroups]":
        "state,simpleConsumerGroup,members,offsets,authorizedOperations,coordinator,partitionAssignor",
      "page[size]": params.pageSize,
      "page[after]": params.pageCursor,
      sort: params.sort
        ? (params.sortDir !== "asc" ? "-" : "") + params.sort
        : undefined,
    }),
  );
  const cgQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/consumerGroups?${cgQuery}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
    next: {
      tags: [`consumer-groups`],
    },
  });
  log.debug({ url }, "getConsumerGroups");
  const rawData = await res.json();
  log.debug({ url, rawData }, "getConsumerGroups response");
  return ConsumerGroupsResponseSchema.parse(rawData);
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
): Promise<ConsumerGroupsResponse> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[consumerGroups]":
        "state,simpleConsumerGroup,members,offsets,authorizedOperations,coordinator,partitionAssignor",
      "page[size]": params.pageSize,
      "page[after]": params.pageCursor,
      sort: params.sort
        ? (params.sortDir !== "asc" ? "-" : "") + params.sort
        : undefined,
    }),
  );
  const cgQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}/consumerGroups?${cgQuery}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
    next: {
      tags: [`consumer-group-${topicId}`],
    },
  });
  log.debug({ url }, "getTopicConsumerGroups");
  const rawData = await res.json();
  log.debug({ url, rawData }, "getTopicConsumerGroups response");
  return ConsumerGroupsResponseSchema.parse(rawData);
}
