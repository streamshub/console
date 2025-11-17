import { ApiErrorSchema } from "@/api/api";
import { NodeSchema } from "@/api/nodes/schema";
import { z } from "zod";

const ConsumerGroupStateSchema = z.union([
  z.literal("UNKNOWN"),
  z.literal("PREPARING_REBALANCE"),
  z.literal("COMPLETING_REBALANCE"),
  z.literal("STABLE"),
  z.literal("DEAD"),
  z.literal("EMPTY"),
  z.literal("ASSIGNING"),
  z.literal("RECONCILING"),
]);

const OffsetAndMetadataSchema = z.object({
  topicId: z.string().optional(),
  topicName: z.string(),
  partition: z.number(),
  offset: z.number().nullable(),
  logEndOffset: z.number().optional(),
  lag: z.number().optional(),
  metadata: z.string().optional(),
  leaderEpoch: z.number().optional(),
});
export type OffsetAndMetadata = z.infer<typeof OffsetAndMetadataSchema>;

const PartitionKeySchema = z.object({
  topicId: z.string().optional(),
  topicName: z.string(),
  partition: z.number(),
});

const MemberDescriptionSchema = z.object({
  memberId: z.string(),
  groupInstanceId: z.string().nullable().optional(),
  clientId: z.string(),
  host: z.string(),
  assignments: z.array(PartitionKeySchema).optional(),
});
export type MemberDescription = z.infer<typeof MemberDescriptionSchema>;

export const ConsumerGroupSchema = z.object({
  id: z.string(),
  type: z.literal("consumerGroups"),
  meta: z
    .object({
      privileges: z.array(z.string()).optional(),
    })
    .optional(),
  attributes: z.object({
    groupId: z.string(),
    simpleConsumerGroup: z.boolean().optional(),
    state: ConsumerGroupStateSchema,
    members: z.array(MemberDescriptionSchema).nullable().optional(),
    partitionAssignor: z.string().nullable().optional(),
    coordinator: NodeSchema.nullable().optional(),
    authorizedOperations: z.array(z.string()).nullable().nullable().optional(),
    offsets: z.array(OffsetAndMetadataSchema).nullable().optional(),
    errors: z.array(ApiErrorSchema).optional(),
  }),
});

export type ConsumerGroup = z.infer<typeof ConsumerGroupSchema>;
export type ConsumerGroupState = z.infer<typeof ConsumerGroupStateSchema>;

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

export const ConsumerGroupResponseSchema = z.object({
  data: ConsumerGroupSchema,
});
export type ConsumerGroupResponse = z.infer<
  typeof ConsumerGroupsResponseSchema
>;
