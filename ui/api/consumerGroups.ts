import { ApiError, getHeaders } from "@/api/api";
import { NodeSchema } from "@/api/kafka";
import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { logger } from "@/utils/logger";
import { z } from "zod";

const log = logger.child({ module: "topics-api" });

const OffsetAndMetadataSchema = z.object({
  topicId: z.string(),
  partition: z.number(),
  offset: z.number(),
  lag: z.number(),
  metadata: z.string(),
  leaderEpoch: z.number().optional(),
});

const PartitionKeySchema = z.object({
  topicId: z.string(),
  topicName: z.string(),
  partition: z.number(),
});

const MemberDescriptionSchema = z.object({
  memberId: z.string(),
  groupInstanceId: z.string().nullable().optional(),
  clientId: z.string(),
  host: z.string(),
  assignments: z.array(PartitionKeySchema),
});

export const ConsumerGroupSchema = z.object({
  id: z.string(),
  type: z.literal("consumerGroups"),
  attributes: z.object({
    simpleConsumerGroup: z.boolean(),
    state: z.string(),
    members: z.array(MemberDescriptionSchema),
    partitionAssignor: z.string().nullable(),
    coordinator: NodeSchema,
    authorizedOperations: z.array(z.string()),
    offsets: z.array(OffsetAndMetadataSchema),
    errors: z.array(ApiError).optional(),
  }),
});
export type ConsumerGroup = z.infer<typeof ConsumerGroupSchema>;
export const ConsumerGroupsResponseSchema = z.object({
  meta: z.object({
    page: z.object({
      total: z.number().optional(),
      pageNumber: z.number().optional(),
    }),
  }),
  links: z.object({
    first: z.string().nullable(),
    prev: z.string().nullable(),
    next: z.string().nullable(),
    last: z.string().nullable(),
  }),
  data: z.array(ConsumerGroupSchema),
});
export type ConsumerGroupsResponse = z.infer<
  typeof ConsumerGroupsResponseSchema
>;

export async function getConsumerGroups(
  kafkaId: string,
  params: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
  },
): Promise<ConsumerGroupsResponse> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[consumerGroups]":
        "state,simpleConsumerGroup,members,offsets,authorizedOperations,coordinator,partitionAssignor",
      "page[size]": params.pageSize,
      "page[after]": params.pageCursor,
      sort: params.sort
        ? (params.sortDir !== "asc" ? "-" : "") + params.sort
        : undefined,
    }),
  );
  const cgQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/consumerGroups?${cgQuery}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
    next: {
      tags: [`consumer-groups`],
    },
  });
  log.debug({ url }, "getConsumerGroups");
  const rawData = await res.json();
  log.trace({ url, rawData }, "getConsumerGroups response");
  return ConsumerGroupsResponseSchema.parse(rawData);
}

export async function getTopicConsumerGroups(
  kafkaId: string,
  topicId: string,
  params: {
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
  },
): Promise<ConsumerGroupsResponse> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[consumerGroups]":
        "state,simpleConsumerGroup,members,offsets,authorizedOperations,coordinator,partitionAssignor",
      "page[size]": params.pageSize,
      "page[after]": params.pageCursor,
      sort: params.sort
        ? (params.sortDir !== "asc" ? "-" : "") + params.sort
        : undefined,
    }),
  );
  const cgQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/topics/${topicId}/consumerGroups?${cgQuery}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
    next: {
      tags: [`consumer-group-${topicId}`],
    },
  });
  log.debug({ url }, "getTopicConsumerGroups");
  const rawData = await res.json();
  log.trace({ url, rawData }, "getTopicConsumerGroups response");
  return ConsumerGroupsResponseSchema.parse(rawData);
}
