"use server";
import { getHeaders } from "@/api/api";
import { Message, MessageApiResponse } from "@/api/messages/schema";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { logger } from "@/utils/logger";

const log = logger.child({ module: "messages-api" });

export type GetTopicMessagesReturn = {
  messages?: Message[];
  ts?: Date;
  error?: "topic-not-found" | "unknown";
};

export async function getTopicMessages(
  kafkaId: string,
  topicId: string,
  params: {
    pageSize: number;
    partition?: number;
    query?: string;
    where?: "key" | "headers" | "value" | `jq:${string}`;
    filter:
      | {
          type: "offset";
          value: number;
        }
      | {
          type: "timestamp";
          value: string;
        }
      | {
          type: "epoch";
          value: number;
        }
      | undefined;
    maxValueLength?: number;
  },
): Promise<GetTopicMessagesReturn> {
  let timestamp: string | undefined;
  try {
    if (params.filter?.type === "epoch") {
      const maybeEpoch = params.filter.value;
      const maybeDate = Number.isInteger(maybeEpoch)
        ? maybeEpoch * 1000
        : params.filter.value;
      const date = maybeDate ? new Date(maybeDate) : undefined;
      timestamp = date?.toISOString();
    }
    if (params.filter?.type === "timestamp") {
      const maybeDate = params.filter.value;
      const date = maybeDate ? new Date(maybeDate) : undefined;
      timestamp = date?.toISOString();
    }
  } catch {}
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[records]":
        "partition,offset,timestamp,timestampType,headers,key,value,size",
      "filter[partition]": params.partition,
      "filter[offset]":
        params.filter?.type === "offset"
          ? "gte," + params.filter?.value
          : undefined,
      "filter[timestamp]": timestamp ? "gte," + timestamp : undefined,
      "page[size]": params.pageSize,
      // maxValueLength: Math.min(params.maxValueLength || 150, 50000),
    }),
  );
  const consumeRecordsQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}/records?${consumeRecordsQuery}`;
  log.info(
    { url, query: Object.fromEntries(sp.entries()), params },
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

    const query = params.query?.toLowerCase();
    const where = params.where;
    if (query !== undefined && query !== null && query.length > 0) {
      const filteredMessages = messages.filter(
        (m) =>
          ((where === "key" || where === undefined) &&
            m.attributes.key?.toLowerCase().includes(query)) ||
          ((where === "value" || where === undefined) &&
            m.attributes.value?.toLowerCase().includes(query)) ||
          ((where === "headers" || where === undefined) &&
            JSON.stringify(m.attributes.headers).toLowerCase().includes(query)),
      );
      log.trace({ filteredMessages, query: params.query }, "Filtered messages");
      return { messages: filteredMessages, ts: new Date() };
    } else {
      return { messages: messages, ts: new Date() };
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

  log.debug({ liveMessages: messages }, "getTopicMessage response");

  return messages?.length === 1 ? messages[0] : undefined;
}
