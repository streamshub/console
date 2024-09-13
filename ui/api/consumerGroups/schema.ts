import { ApiError } from "@/api/api";
import { NodeSchema } from "@/api/kafka/schema";
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
  topicId: z.string(),
  topicName: z.string(),
  partition: z.number(),
  offset: z.number(),
  logEndOffset: z.number().optional(),
  lag: z.number().optional(),
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
  assignments: z.array(PartitionKeySchema).optional(),
});
export const ConsumerGroupSchema = z.object({
  id: z.string(),
  type: z.literal("consumerGroups"),
  attributes: z.object({
    simpleConsumerGroup: z.boolean().optional(),
    state: ConsumerGroupStateSchema,
    members: z.array(MemberDescriptionSchema).optional(),
    partitionAssignor: z.string().nullable().optional(),
    coordinator: NodeSchema.nullable().optional(),
    authorizedOperations: z.array(z.string()).nullable().optional(),
    offsets: z.array(OffsetAndMetadataSchema).optional(),
    errors: z.array(ApiError).optional(),
  }),
});

const DryrunOffsetSchema = z.object({
  topicId: z.string(),
  topicName: z.string(),
  partition: z.number(),
  offset: z.number(),
  metadata: z.string(),
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

export const DryrunSchema = z.object({
  id: z.string(),
  type: z.literal("consumerGroups"),
  attributes: z.object({
    state: ConsumerGroupStateSchema,
    members: z.array(MemberDescriptionSchema).optional(),
    offsets: z.array(DryrunOffsetSchema).optional(),
  }),
});

export const UpdateConsumerGroupErrorSchema = z.object({
  errors: z.array(
    z.object({
      id: z.string(),
      status: z.string(),
      code: z.string(),
      title: z.string(),
      detail: z.string(),
      source: z
        .object({
          pointer: z.string().optional(),
        })
        .optional(),
    }),
  ),
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

export type UpdateConsumerGroupErrorSchema = z.infer<
  typeof UpdateConsumerGroupErrorSchema
>;

export const ConsumerGroupDryrunResponseSchema = z.object({
  data: DryrunSchema,
});

export type DryrunResponse = z.infer<typeof DryrunSchema>;
