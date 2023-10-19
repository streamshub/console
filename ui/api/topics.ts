import { BackendError, getHeaders } from "@/api/api";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { logger } from "@/utils/logger";
import { z } from "zod";

const log = logger.child({ module: "topics-api" });

const describeTopicsQuery = encodeURI(
  "fields[topics]=,name,internal,partitions,authorizedOperations,configs,recordCount,totalLeaderLogBytes",
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
      localStorage: BackendError.or(
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
  z.union([z.string(), z.number(), z.null()]),
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
});
export type TopicList = z.infer<typeof TopicListSchema>;
export const TopicsResponse = z.object({
  meta: z.object({
    page: z.object({
      total: z.number(),
    }),
  }),
  data: z.array(TopicListSchema),
});
export type TopicsResponseList = z.infer<typeof TopicsResponse>;

const TopicCreateResponseSuccessSchema = z.object({
  data: z.object({
    id: z.string(),
  }),
});
const TopicCreateResponseErrorSchema = z.object({
  errors: z.array(
    z.object({
      id: z.string(),
      status: z.string(),
      code: z.string(),
      title: z.string(),
      detail: z.string(),
      source: z
        .object({
          pointer: z.string().nullable(),
        })
        .nullable(),
    }),
  ),
});
const TopicCreateResponseSchema = z.union([
  TopicCreateResponseSuccessSchema,
  TopicCreateResponseErrorSchema,
]);
export type TopicCreateError = z.infer<typeof TopicCreateResponseErrorSchema>;
export type TopicCreateResponse = z.infer<typeof TopicCreateResponseSchema>;

export async function getTopics(
  kafkaId: string,
  params: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
  },
): Promise<TopicsResponseList> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[topics]":
        "name,internal,partitions,recordCount,totalLeaderLogBytes",
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
  });
  log.debug({ url }, "getTopics");
  const rawData = await res.json();
  log.trace({ url, rawData }, "getTopics response");
  return TopicsResponse.parse(rawData);
}

export async function getTopic(
  kafkaId: string,
  topicId: string,
): Promise<Topic> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}?${describeTopicsQuery}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
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
): Promise<TopicCreateResponse> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics`;
  const body = JSON.stringify({
    data: {
      type: "topics",
      meta: {},
      attributes: {
        name,
        numPartitions,
        replicationFactor,
        configs,
      },
    },
  });
  const res = await fetch(url, {
    headers: await getHeaders(),

    method: "POST",
    body,
  });
  log.trace({ url, body }, "calling createTopic");
  const rawData = await res.json();
  log.debug({ url, rawData }, "createTopic response");
  const response = TopicCreateResponseSchema.parse(rawData);
  if ("data" in response) {
    return response;
  }
  return response;
}
