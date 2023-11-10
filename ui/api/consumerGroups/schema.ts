import { ApiError } from "@/api/api";
import { NodeSchema } from "@/api/kafka/schema";
import { z } from "zod";

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
