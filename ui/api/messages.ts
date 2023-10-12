import { getHeaders } from "@/api/api";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { logger } from "@/utils/logger";
import { z } from "zod";

const log = logger.child({ module: "messages-api" });

const MessageSchema = z.object({
  type: z.literal("records"),
  attributes: z.object({
    partition: z.number(),
    offset: z.number(),
    timestamp: z.string(),
    timestampType: z.string(),
    headers: z.record(z.any()),
    key: z.string().nullable(),
    value: z.string().nullable(),
  }),
});
const MessageApiResponse = z.object({
  meta: z.object({}).nullable(),
  data: z.array(MessageSchema),
});
export type Message = z.infer<typeof MessageSchema>;

export async function getTopicMessages(
  kafkaId: string,
  topicId: string,
  params: {
    pageSize: number;
    partition: number | undefined;
    filter:
      | {
          type: "offset";
          value: number;
        }
      | {
          type: "timestamp";
          value: string;
        }
      | undefined;
  },
): Promise<Message[]> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[records]":
        "partition,offset,timestamp,timestampType,headers,key,value",
      "filter[partition]": params.partition,
      "filter[offset]":
        params.filter?.type === "offset"
          ? "gte," + params.filter?.value
          : undefined,
      "filter[timestamp]":
        params.filter?.type === "timestamp"
          ? "gte," + params.filter?.value
          : undefined,
      "page[size]": params.pageSize,
    }),
  );
  const consumeRecordsQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}/records?${consumeRecordsQuery}`;
  log.debug({ url }, "Fetching topic messages");
  const res = await fetch(url, {
    headers: await getHeaders(),
    cache: "no-store",
    next: { tags: [`messages-${topicId}`] },
  });
  const rawData = await res.json();
  log.trace(rawData, "Received messages");
  return MessageApiResponse.parse(rawData).data;
}
