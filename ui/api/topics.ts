import { ApiError, getHeaders } from "@/api/api";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { logger } from "@/utils/logger";
import { revalidateTag } from "next/cache";
import { z } from "zod";

const log = logger.child({ module: "topics-api" });

const describeTopicsQuery = encodeURI(
  "fields[topics]=,name,internal,partitions,authorizedOperations,configs,recordCount,totalLeaderLogBytes,consumerGroups",
);

const OffsetSchema = z.object({
  offset: z.number().optional(),
  timestamp: z.string().optional(),
  leaderEpoch: z.number().optional(),
});
const PartitionSchema = z.object({
  partition: z.number(),
  leaderId: z.number(),
  replicas: z.array(
    z.object({
      nodeId: z.number(),
      nodeRack: z.string().optional(),
      inSync: z.boolean(),
      localStorage: ApiError.or(
        z.object({
          size: z.number(),
          offsetLag: z.number(),
          future: z.boolean(),
        }),
      ).optional(),
    }),
  ),
  offsets: z
    .object({
      earliest: OffsetSchema.optional(),
      latest: OffsetSchema.optional(),
      maxTimestamp: OffsetSchema.optional(),
      timestamp: OffsetSchema.optional(),
    })
    .optional()
    .nullable(),
  recordCount: z.number().optional(),
  leaderLocalStorage: z.number().optional(),
});
const ConfigSchema = z.object({
  value: z.string(),
  source: z.string(),
  sensitive: z.boolean(),
  readOnly: z.boolean(),
  type: z.string(),
});

const ConfigMapSchema = z.record(z.string(), ConfigSchema);
export type ConfigMap = z.infer<typeof ConfigMapSchema>;
const NewConfigMapSchema = z.record(
  z.string(),
  z.object({
    value: z.union([z.string(), z.number(), z.undefined(), z.null()]),
  }),
);
export type NewConfigMap = z.infer<typeof NewConfigMapSchema>;

const TopicSchema = z.object({
  id: z.string(),
  type: z.literal("topics"),
  attributes: z.object({
    name: z.string(),
    internal: z.boolean(),
    partitions: z.array(PartitionSchema),
    authorizedOperations: z.array(z.string()),
    configs: ConfigMapSchema,
    recordCount: z.number().optional(),
    totalLeaderLogBytes: z.number().optional(),
  }),
  relationships: z.object({
    consumerGroups: z.object({
      data: z.array(z.any()),
    }),
  }),
});
export const TopicResponse = z.object({
  data: TopicSchema,
});
export type Topic = z.infer<typeof TopicSchema>;

const TopicListSchema = z.object({
  id: z.string(),
  type: z.literal("topics"),
  meta: z.object({
    page: z.object({
      cursor: z.string(),
    }),
  }),
  attributes: TopicSchema.shape.attributes.pick({
    name: true,
    internal: true,
    partitions: true,
    recordCount: true,
    totalLeaderLogBytes: true,
  }),
  relationships: z.object({
    consumerGroups: z.object({
      data: z.array(z.any()),
    }),
  }),
});
export type TopicList = z.infer<typeof TopicListSchema>;
export const TopicsResponseSchema = z.object({
  meta: z.object({
    page: z.object({
      total: z.number(),
      pageNumber: z.number(),
    }),
  }),
  links: z.object({
    first: z.string().nullable(),
    prev: z.string().nullable(),
    next: z.string().nullable(),
    last: z.string().nullable(),
  }),
  data: z.array(TopicListSchema),
});
export type TopicsResponse = z.infer<typeof TopicsResponseSchema>;

const TopicCreateResponseSuccessSchema = z.object({
  data: z.object({
    id: z.string(),
  }),
});
const TopicMutateResponseErrorSchema = z.object({
  errors: z.array(
    z.object({
      id: z.string(),
      status: z.string(),
      code: z.string(),
      title: z.string(),
      detail: z.string(),
      source: z
        .object({
          pointer: z.string().optional(),
        })
        .optional(),
    }),
  ),
});
const TopicCreateResponseSchema = z.union([
  TopicCreateResponseSuccessSchema,
  TopicMutateResponseErrorSchema,
]);
export type TopicMutateError = z.infer<typeof TopicMutateResponseErrorSchema>;
export type TopicCreateResponse = z.infer<typeof TopicCreateResponseSchema>;

export async function getTopics(
  kafkaId: string,
  params: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
  },
): Promise<TopicsResponse> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[topics]":
        "name,internal,partitions,recordCount,totalLeaderLogBytes,consumerGroups",
      "page[size]": params.pageSize,
      "page[after]": params.pageCursor,
      sort: params.sort
        ? (params.sortDir !== "asc" ? "-" : "") + params.sort
        : undefined,
    }),
  );
  const topicsQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics?${topicsQuery}&`;
  const res = await fetch(url, {
    headers: await getHeaders(),
    next: {
      tags: ["topics"],
    },
  });
  log.debug({ url }, "getTopics");
  const rawData = await res.json();
  log.trace({ url, rawData }, "getTopics response");
  return TopicsResponseSchema.parse(rawData);
}

export async function getTopic(
  kafkaId: string,
  topicId: string,
): Promise<Topic> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}?${describeTopicsQuery}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
    next: {
      tags: [`topic-${topicId}`],
    },
  });
  const rawData = await res.json();
  //log.debug("getTopic", url, JSON.stringify(rawData, null, 2));
  return TopicResponse.parse(rawData).data;
}

export async function createTopic(
  kafkaId: string,
  name: string,
  numPartitions: number,
  replicationFactor: number,
  configs: NewConfigMap,
  validateOnly = false,
): Promise<TopicCreateResponse> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics`;
  const body = {
    meta: {
      validateOnly,
    },
    data: {
      type: "topics",
      attributes: {
        name,
        numPartitions,
        replicationFactor,
        configs: filterUndefinedFromObj(configs),
      },
    },
  };
  log.debug({ url, body }, "calling createTopic");
  const res = await fetch(url, {
    headers: await getHeaders(),
    method: "POST",
    body: JSON.stringify(body),
  });
  const rawData = await res.json();
  log.debug({ url, rawData }, "createTopic response");
  const response = TopicCreateResponseSchema.parse(rawData);
  log.trace(response, "createTopic response parsed");
  if (validateOnly === false) {
    revalidateTag("topics");
  }
  return response;
}

export async function updateTopic(
  kafkaId: string,
  topicId: string,
  numPartitions?: number,
  replicationFactor?: number,
  configs?: NewConfigMap,
): Promise<boolean | TopicMutateError> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}`;
  const body = {
    data: {
      type: "topics",
      id: topicId,
      attributes: {
        numPartitions,
        replicationFactor,
        configs: filterUndefinedFromObj(configs || {}),
      },
    },
  };
  log.debug({ url, body }, "calling updateTopic");
  const res = await fetch(url, {
    headers: await getHeaders(),
    method: "PATCH",
    body: JSON.stringify(body),
  });
  log.trace({ status: res.status }, "updateTopic response");
  try {
    if (res.status === 204) {
      revalidateTag(`topic-${topicId}`);
      return true;
    } else {
      const rawData = await res.json();
      return TopicMutateResponseErrorSchema.parse(rawData);
    }
  } catch (e) {
    log.error(e, "deleteTopic unknown error");
  }
  return false;
}

export async function deleteTopic(
  kafkaId: string,
  topicId: string,
): Promise<boolean> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}`;
  log.debug({ url }, "calling deleteTopic");
  const res = await fetch(url, {
    headers: await getHeaders(),
    method: "DELETE",
  });
  try {
    const success = res.status === 204;
    if (success) {
      revalidateTag("topics");
    }
    return success;
  } catch (e) {
    log.error(e, "deleteTopic unknown error");
  }
  return false;
}
