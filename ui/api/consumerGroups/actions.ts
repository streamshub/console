"use server";
import { getHeaders } from "@/api/api";
import {
  ConsumerGroup,
  ConsumerGroupDryrunResponseSchema,
  ConsumerGroupResponseSchema,
  ConsumerGroupsResponse,
  ConsumerGroupsResponseSchema,
  ConsumerGroupState,
  DryrunResponse,
  UpdateConsumerGroupErrorSchema,
} from "@/api/consumerGroups/schema";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { logger } from "@/utils/logger";

const log = logger.child({ module: "consumergroup-api" });

export async function getConsumerGroup(
  kafkaId: string,
  groupId: string,
): Promise<ConsumerGroup> {
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
    fields?: string;
    id?: string;
    state?: ConsumerGroupState[];
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
  },
): Promise<ConsumerGroupsResponse | null> {
  try {
    const sp = new URLSearchParams(
      filterUndefinedFromObj({
        "fields[consumerGroups]":
          params.fields ?? "state,simpleConsumerGroup,members,offsets",
        "filter[id]": params.id ? `eq,${params.id}` : undefined,
        // TODO: pass filter from UI
        "filter[state]":
          params.state && params.state.length > 0
            ? `in,${params.state.join(",")}`
            : undefined,
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
    if (res.status === 200) {
      const rawData = await res.json();
      log.debug({ url, rawData }, "getConsumerGroups response");
      return ConsumerGroupsResponseSchema.parse(rawData);
    }
  } catch (err) {
    log.error(err, "getConsumerGroups");
    throw new Error("getConsumerGroups: couldn't connect with backend");
  }
  return null;
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

export async function updateConsumerGroup(
  kafkaId: string,
  consumerGroupId: string,
  offsets: Array<{
    topicId: string;
    partition?: number;
    offset: string | number;
    metadata?: string;
  }>,
): Promise<boolean | UpdateConsumerGroupErrorSchema> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/consumerGroups/${consumerGroupId}`;
  const body = {
    data: {
      type: "consumerGroups",
      id: consumerGroupId,
      attributes: {
        offsets,
      },
    },
  };

  log.debug({ url, body }, "calling updateConsumerGroup");

  try {
    const res = await fetch(url, {
      headers: await getHeaders(),
      method: "PATCH",
      body: JSON.stringify(body),
    });

    log.debug({ status: res.status }, "updateConsumerGroup response");

    if (res.status === 204) {
      return true;
    } else {
      const rawData = await res.json();
      return UpdateConsumerGroupErrorSchema.parse(rawData);
    }
  } catch (e) {
    log.error(e, "updateConsumerGroup unknown error");
    console.error("Unknown error occurred:", e);
    return false;
  }
}

export async function getDryrunResult(
  kafkaId: string,
  consumerGroupId: string,
  offsets: Array<{
    topicId: string;
    partition?: number;
    offset: string | number;
    metadata?: string;
  }>,
): Promise<DryrunResponse> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/consumerGroups/${consumerGroupId}`;
  const body = {
    meta: {
      dryRun: true,
    },
    data: {
      type: "consumerGroups",
      id: consumerGroupId,
      attributes: {
        offsets,
      },
    },
  };
  const res = await fetch(url, {
    headers: await getHeaders(),
    method: "PATCH",
    body: JSON.stringify(body),
  });
  const rawData = await res.json();
  log.debug({ url, rawData }, "getConsumerGroup response");
  return ConsumerGroupDryrunResponseSchema.parse(rawData).data;
}
