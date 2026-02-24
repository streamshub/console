"use server";
import {
  fetchData,
  patchData,
  postData,
  getHeaders,
  ApiResponse,
  filterEq,
  filterIn,
  filterLike,
  sortParam,
} from "@/api/api";
import { getKafkaCluster } from "@/api/kafka/actions";
import {
  describeTopicsQuery,
  NewConfigMap,
  Topic,
  TopicCreateResponse,
  TopicCreateResponseSchema,
  TopicMetrics,
  TopicMetricsResponseSchema,
  TopicResponse,
  TopicsResponse,
  TopicsResponseSchema,
  TopicStatus,
} from "@/api/topics/schema";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { logger } from "@/utils/logger";
import { getSession, setSession } from "@/utils/session";

const log = logger.child({ module: "topics-api" });

export async function getTopics(
  kafkaId: string,
  params: {
    name?: string;
    id?: string;
    fields?: string;
    status?: TopicStatus[];
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
    includeHidden?: boolean;
  },
): Promise<ApiResponse<TopicsResponse>> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[topics]":
        params.fields ??
        "name,status,visibility,numPartitions,totalLeaderLogBytes,groups",
      "filter[id]": filterEq(params.id),
      "filter[name]": filterLike(params.name),
      "filter[status]": filterIn(params.status),
      "filter[visibility]": params.includeHidden
        ? "in,external,internal"
        : "eq,external",
      "page[size]": params.pageSize,
      "page[after]": params.pageCursor?.startsWith("after:")
        ? params.pageCursor.slice(6)
        : undefined,
      "page[before]": params.pageCursor?.startsWith("before:")
        ? params.pageCursor.slice(7)
        : undefined,
      sort: sortParam(params.sort, params.sortDir),
    }),
  );

  return fetchData(`/api/kafkas/${kafkaId}/topics`, sp, (rawData: any) =>
    TopicsResponseSchema.parse(rawData),
  );
}

export async function getTopic(
  kafkaId: string,
  topicId: string,
): Promise<ApiResponse<Topic>> {
  return fetchData(
    `/api/kafkas/${kafkaId}/topics/${topicId}`,
    describeTopicsQuery,
    (rawData: any) => TopicResponse.parse(rawData).data,
  );
}

export async function createTopic(
  kafkaId: string,
  name: string,
  numPartitions: number,
  replicationFactor: number,
  configs: NewConfigMap,
  validateOnly = false,
): Promise<ApiResponse<TopicCreateResponse>> {
  return postData(
    `/api/kafkas/${kafkaId}/topics`,
    {
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
    },
    (rawData) => TopicCreateResponseSchema.parse(rawData),
  );
}

export async function updateTopic(
  kafkaId: string,
  topicId: string,
  numPartitions?: number,
  replicationFactor?: number,
  configs?: NewConfigMap,
): Promise<ApiResponse<undefined>> {
  return patchData(
    `/api/kafkas/${kafkaId}/topics/${topicId}`,
    {
      data: {
        type: "topics",
        id: topicId,
        attributes: {
          numPartitions,
          replicationFactor,
          configs: filterUndefinedFromObj(configs ?? {}),
        },
      },
    },
    (_) => undefined,
  );
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
      log.debug({ url }, "deleteTopic success");
    }
    return success;
  } catch (e) {
    log.error({ err: e, url }, "deleteTopic unknown error");
  }
  return false;
}

type ViewedTopicsSession = { viewedTopics: ViewedTopic[] | undefined };
export type ViewedTopic = {
  kafkaId: string;
  kafkaName: string;
  topicId: string;
  topicName: string;
};

export async function getViewedTopics(): Promise<ViewedTopic[]> {
  log.debug("getViewedTopics");
  const recentTopicsSession =
    await getSession<ViewedTopicsSession>("recent-topics");
  log.trace(recentTopicsSession, "getViewedTopics session");
  return recentTopicsSession.viewedTopics || [];
}

export async function setTopicAsViewed(kafkaId: string, topicId: string) {
  log.trace({ kafkaId, topicId }, "setTopicAsViewed");
  const cluster = (await getKafkaCluster(kafkaId)).payload;
  const topic = (await getTopic(kafkaId, topicId)).payload;
  const viewedTopics = await getViewedTopics();

  if (cluster && topic) {
    const viewedTopic: ViewedTopic = {
      kafkaId,
      kafkaName: cluster.attributes.name,
      topicId,
      // name is included in the `fields[topics]` param list so we are sure it is present
      topicName: topic.attributes.name!,
    };
    if (viewedTopics.find((t) => t.topicId === viewedTopic.topicId)) {
      log.trace(
        { kafkaId, topicId },
        "setTopicAsViewed: topic was already in the list, ignoring",
      );
      return viewedTopics;
    }
    log.trace(
      { kafkaId, topicId },
      "setTopicAsViewed: adding topic to the list",
    );
    const updatedViewedTopics = [viewedTopic, ...viewedTopics].slice(0, 5);
    await setSession<ViewedTopicsSession>("recent-topics", {
      viewedTopics: updatedViewedTopics,
    });
    log.trace(updatedViewedTopics, "setTopicAsViewed: updated list");
    return updatedViewedTopics;
  } else {
    log.trace({ topic, cluster }, "setTopicAsViewed: invalid topic/cluster");
    return viewedTopics;
  }
}

export async function gettopicMetrics(
  kafkaId: string,
  topicId: number | string,
  duration: number,
): Promise<ApiResponse<TopicMetrics>> {
  const queryParams = new URLSearchParams({
    "duration[metrics]": duration.toString(),
  });

  return fetchData(
    `/api/kafkas/${kafkaId}/topics/${topicId}/metrics`,
    queryParams,
    (rawData) => TopicMetricsResponseSchema.parse(rawData),
  );
}
