"use server";
import { fetchData, filterGte, ApiResponse } from "@/api/api";
import { Message, MessageApiResponse } from "@/api/messages/schema";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { logger } from "@/utils/logger";

const RECORD_FIELDS = "partition,offset,timestamp,timestampType,headers,key,keySchema,value,valueSchema,size";
const log = logger.child({ module: "messages-api" });

function getTimestampFilter(
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
    | undefined
) {
  let timestamp: string | undefined;
  try {
    if (filter?.type === "epoch") {
      const maybeEpoch = filter.value;
      const maybeDate = Number.isInteger(maybeEpoch)
        ? maybeEpoch * 1000
        : filter.value;
      const date = maybeDate ? new Date(maybeDate) : undefined;
      timestamp = date?.toISOString();
    }
    if (filter?.type === "timestamp") {
      const maybeDate = filter.value;
      const date = maybeDate ? new Date(maybeDate) : undefined;
      timestamp = date?.toISOString();
    }
  } catch {}

  return timestamp;
}

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
): Promise<ApiResponse<Message[]>> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[records]": RECORD_FIELDS,
      "filter[partition]": params.partition,
      "filter[offset]":
        params.filter?.type === "offset"
          ? "gte," + params.filter?.value
          : undefined,
      "filter[timestamp]": filterGte(getTimestampFilter(params.filter)),
      "page[size]": params.pageSize,
      // maxValueLength: Math.min(params.maxValueLength || 150, 50000),
    }),
  );

  const response = fetchData(
    `/api/kafkas/${kafkaId}/topics/${topicId}/records`,
    sp,
    (rawData) => MessageApiResponse.parse(rawData).data
  );

  return response.then(resp => {
    const query = params.query?.toLowerCase() ?? "";

    if (resp.payload && query.length > 0) {
      const where = params.where;
      const filteredMessages = resp.payload.filter(
        (m) =>
          ((where === "key" || where === undefined) &&
            m.attributes.key?.toLowerCase().includes(query)) ||
          ((where === "value" || where === undefined) &&
            m.attributes.value?.toLowerCase().includes(query)) ||
          ((where === "headers" || where === undefined) &&
            JSON.stringify(m.attributes.headers).toLowerCase().includes(query)),
      );

      log.trace({ filteredMessages, query: params.query }, "Filtered messages");
      resp.payload = filteredMessages;
    }

    return resp;
  });
}

export async function getTopicMessage(
  kafkaId: string,
  topicId: string,
  params: {
    partition: number;
    offset: number;
  },
): Promise<ApiResponse<Message | undefined>> {
  const sp = new URLSearchParams({
      "fields[records]": RECORD_FIELDS,
      "filter[partition]": String(params.partition),
      "filter[offset]": "gte," + params.offset,
      "page[size]": "1",
  });

  return fetchData(
    `/api/kafkas/${kafkaId}/topics/${topicId}/records`,
    sp,
    (rawData) => {
      const messages = MessageApiResponse.parse(rawData).data;
      return messages.length === 1 ? messages[0] : undefined;
    }
  );
}
