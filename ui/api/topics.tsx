import { z } from "zod";

const PartitionSchema = z.object({
  partition: z.number(),
  leader: z.object({
    id: z.number(),
    host: z.string(),
    port: z.number(),
  }),
  replicas: z.array(
    z.object({
      id: z.number(),
      host: z.string(),
      port: z.number(),
    }),
  ),
  isr: z.array(
    z.object({
      id: z.number(),
      host: z.string(),
      port: z.number(),
    }),
  ),
  offset: z.object({
    offset: z.number(),
    leaderEpoch: z.number(),
  }),
});

const ConfigSchema = z.object({
  value: z.string(),
  source: z.string(),
  sensitive: z.boolean(),
  readOnly: z.boolean(),
  type: z.string(),
});

const TopicSchema = z.object({
  id: z.string(),
  type: z.string(),
  attributes: z.object({
    name: z.string(),
    internal: z.boolean(),
    partitions: z.array(PartitionSchema),
    authorizedOperations: z.array(z.string()),
    configs: z.record(z.string(), ConfigSchema),
  }),
});

const TopicsResponse = z.object({
  data: z.array(TopicSchema),
});

const TopicResponse = z.object({
  data: TopicSchema,
});

export type Topic = z.infer<typeof TopicSchema>;

export async function getTopics(kafkaId: string): Promise<Topic[]> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics?fields%5Btopics%5D=name,internal,partitions,authorizedOperations,configs&offsetSpec=latest`;
  const res = await fetch(url, {
    headers: {
      Accept: "application/json",
    },
  });
  const rawData = await res.json();
  return TopicsResponse.parse(rawData).data;
}

export async function getTopic(
  kafkaId: string,
  topicId: string,
): Promise<Topic> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}?fields%5Btopics%5D=name,internal,partitions,authorizedOperations,configs&offsetSpec=latest`;
  const res = await fetch(url, {
    headers: {
      Accept: "application/json",
    },
  });
  const rawData = await res.json();
  return TopicResponse.parse(rawData).data;
}
