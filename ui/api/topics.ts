import { BackendError, getHeaders } from "@/api/api";
import { logger } from "@/utils/logger";
import { z } from "zod";

const log = logger.child({ module: "topics-api" });

const listTopicsQuery = encodeURI(
  "fields[topics]=name,internal,partitions,recordCount",
);
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
const ConfigSchemaMapSchema = z.record(z.string(), ConfigSchema);
export type ConfigSchemaMap = z.infer<typeof ConfigSchemaMapSchema>;
const TopicSchema = z.object({
  id: z.string(),
  type: z.literal("topics"),
  attributes: z.object({
    name: z.string(),
    internal: z.boolean(),
    partitions: z.array(PartitionSchema),
    authorizedOperations: z.array(z.string()),
    configs: ConfigSchemaMapSchema,
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
  attributes: TopicSchema.shape.attributes.pick({
    name: true,
    internal: true,
    partitions: true,
    recordCount: true,
  }),
});
export type TopicList = z.infer<typeof TopicListSchema>;
export const TopicsResponse = z.object({
  data: z.array(TopicListSchema),
});

const TopicCreateResponseSchema = z.object({
  data: z.object({
    id: z.string(),
  }),
});
export type TopicCreateResponse = z.infer<typeof TopicCreateResponseSchema>;

export async function getTopics(kafkaId: string): Promise<TopicList[]> {
  "use server";
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics?${listTopicsQuery}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
    cache: "no-store",
  });
  const rawData = await res.json();
  log.debug({ url, rawData }, "getTopics");
  return TopicsResponse.parse(rawData).data;
}

export async function getTopic(
  kafkaId: string,
  topicId: string,
): Promise<Topic> {
  "use server";
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}?${describeTopicsQuery}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
    cache: "no-store",
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
  configs: ConfigSchemaMap,
) {
  "use server";
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
    cache: "no-store",
    method: "POST",
    body,
  });
  log.trace({ url, body }, "calling createTopic");
  const rawData = await res.json();
  log.debug({ url, rawData }, "createTopic response");
  return TopicCreateResponseSchema.parse(rawData).data;
}
