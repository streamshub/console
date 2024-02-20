"use server";
import { getHeaders } from "@/api/api";
import { Message, MessageApiResponse } from "@/api/messages/schema";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { logger } from "@/utils/logger";

const log = logger.child({ module: "messages-api" });

export type GetTopicMessagesReturn = {
  messages: Message[];
  ts: Date;
  error?: "topic-not-found" | "unknown";
};

export async function getTopicMessages(
  kafkaId: string,
  topicId: string,
  params: {
    pageSize: number;
    partition?: number;
    query?: string;
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
): Promise<GetTopicMessagesReturn> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[records]":
        "partition,offset,timestamp,timestampType,headers,key,value,size",
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
      // maxValueLength: Math.min(params.maxValueLength || 150, 50000),
    }),
  );
  const consumeRecordsQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}/records?${consumeRecordsQuery}`;
  log.debug(
    { url, params: Object.fromEntries(sp.entries()) },
    "Fetching topic messages",
  );
  const res = await fetch(url, {
    headers: await getHeaders(),
    cache: "no-store",
    next: { tags: [`messages-${topicId}`] },
  });
  const rawData = await res.json();
  log.trace({ rawData }, "Received messages");
  try {
    const messages = MessageApiResponse.parse(rawData).data;

    if (params.query !== undefined && params.query !== null) {
      const query = params.query;
      const filteredMessages = messages.filter(
        (m) =>
          m.attributes.key?.includes(query) ||
          m.attributes.value?.includes(query),
      );
      log.trace({ filteredMessages, query: params.query }, "Filtered messages");
      return { messages: filteredMessages, ts: new Date() };
    } else {
      return { messages, ts: new Date() };
    }
  } catch {
    log.error(
      { status: res.status, message: rawData, url },
      "Error fetching message",
    );
    if (res.status === 404) {
      return {
        messages: [],
        ts: new Date(),
        error: "topic-not-found",
      };
    }
    return {
      messages: [],
      ts: new Date(),
      error: "unknown",
    };
  }
}

export async function getTopicMessage(
  kafkaId: string,
  topicId: string,
  params: {
    partition: number;
    offset: number;
  },
): Promise<Message | undefined> {
  log.debug({ kafkaId, topicId, params }, "getTopicMessage");
  const { messages } = await getTopicMessages(kafkaId, topicId, {
    pageSize: 1,
    partition: params.partition,
    query: undefined,
    filter: {
      type: "offset",
      value: params.offset,
    },
    maxValueLength: 50000,
  });

  log.debug({ messages }, "getTopicMessage response");

  return messages.length === 1 ? messages[0] : undefined;
}
