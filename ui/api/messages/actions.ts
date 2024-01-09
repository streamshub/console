"use server";
import { getHeaders } from "@/api/api";
import { Message, MessageApiResponse } from "@/api/messages/schema";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { logger } from "@/utils/logger";

const log = logger.child({ module: "messages-api" });

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
    maxValueLength: number | undefined;
  },
): Promise<{ messages: Message[]; ts: Date }> {
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
      maxValueLength: Math.min(params.maxValueLength || 150, 50000),
    }),
  );
  const consumeRecordsQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}/records?${consumeRecordsQuery}`;
  log.info(
    { url, params: Object.fromEntries(sp.entries()) },
    "Fetching topic messages",
  );
  const res = await fetch(url, {
    headers: await getHeaders(),

    next: { tags: [`messages-${topicId}`] },
  });
  const rawData = await res.json();
  log.trace({ rawData }, "Received messages");
  return { messages: MessageApiResponse.parse(rawData).data, ts: new Date() };
}

export async function getTopicMessage(
  kafkaId: string,
  topicId: string,
  params: {
    partition: number;
    offset: number;
  },
): Promise<Message | undefined> {
  log.info({ kafkaId, topicId, params }, "getTopicMessage");
  const { messages } = await getTopicMessages(kafkaId, topicId, {
    pageSize: 1,
    partition: params.partition,
    filter: {
      type: "offset",
      value: params.offset,
    },
    maxValueLength: 50000,
  });

  log.debug({ messages }, "getTopicMessage response");

  return messages.length === 1 ? messages[0] : undefined;
}
