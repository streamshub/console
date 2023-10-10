"use server";
import { Topic, TopicResponse, TopicsResponse } from "@/api/types";
import { logger } from "@/utils/logger";
import { getUser } from "@/utils/session";

const log = logger.child({ module: "topics-api" });

const listTopicsQuery = encodeURI(
  "fields[topics]=name,internal,partitions,authorizedOperations,configs",
);
const describeTopicsQuery = encodeURI(
  "fields[topics]=,name,internal,partitions,authorizedOperations,configs,recordCount,totalLeaderLogBytes",
);
const consumeRecordsQuery = encodeURI(
  "fields[records]=partition,offset,timestamp,timestampType,headers,key,value&page[size]=20",
);

async function getHeaders(): Promise<Record<string, string>> {
  const user = await getUser();
  return {
    Accept: "application/json",
    Authorization: `Bearer ${user.accessToken}`,
  };
}

export async function getTopics(kafkaId: string): Promise<Topic[]> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics?${listTopicsQuery}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
    cache: "no-store",
  });
  const rawData = await res.json();
  log.debug("getTopics", url, JSON.stringify(rawData, null, 2));
  return TopicsResponse.parse(rawData).data;
}

export async function getTopic(
  kafkaId: string,
  topicId: string,
): Promise<Topic> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}?${describeTopicsQuery}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
    cache: "no-store",
  });
  const rawData = await res.json();
  //log.debug("getTopic", url, JSON.stringify(rawData, null, 2));
  return TopicResponse.parse(rawData).data;
}

export async function getTopicMessages(
  kafkaId: string,
  topicId: string,
): Promise<MessageApiResponse> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}/records?${consumeRecordsQuery}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
    cache: "no-store",
    next: { tags: [`messages-${topicId}`] },
  });
  const rawData = await res.json();
  const messages = (rawData?.data || []).map((r: any) => ({ ...r.attributes }));
  return {
    lastUpdated: new Date(),
    messages,
    partitions: 1,
    offsetMin: 0,
    offsetMax: 100,
    filter: {
      partition: undefined,
      offset: undefined,
      timestamp: undefined,
      limit: undefined,
      epoch: undefined,
    },
  };
}

export type MessageApiResponse = {
  lastUpdated: Date;
  messages: Message[];
  partitions: number;
  offsetMin: number;
  offsetMax: number;

  filter: {
    partition: number | undefined;
    offset: number | undefined;
    timestamp: DateIsoString | undefined;
    limit: number | undefined;
    epoch: number | undefined;
  };
};

export type Message = {
  partition?: number;
  offset?: number;
  timestamp?: DateIsoString;
  key?: string;
  value?: string;
  headers: Record<string, string>;
};
