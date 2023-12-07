import { ApiError } from "@/api/api";
import { z } from "zod";

export const describeTopicsQuery = encodeURI(
  "fields[topics]=name,status,visibility,partitions,numPartitions,authorizedOperations,configs,recordCount,totalLeaderLogBytes,consumerGroups",
);
const OffsetSchema = z.object({
  offset: z.number().optional(),
  timestamp: z.string().optional(),
  leaderEpoch: z.number().optional(),
});
const PartitionSchema = z.object({
  partition: z.number(),
  status: z.string(),
  leaderId: z.number().optional(),
  replicas: z.array(
    z.object({
      nodeId: z.number(),
      nodeRack: z.string().optional(),
      inSync: z.boolean(),
      localStorage: ApiError.or(
        z.object({
          size: z.number(),
          offsetLag: z.number(),
          future: z.boolean(),
        }),
      ).optional(),
    }),
  ),
  offsets: z
    .object({
      earliest: OffsetSchema.optional(),
      latest: OffsetSchema.optional(),
      maxTimestamp: OffsetSchema.optional(),
      timestamp: OffsetSchema.optional(),
    })
    .optional()
    .nullable(),
  recordCount: z.number().optional(),
  leaderLocalStorage: z.number().optional(),
});
const ConfigSchema = z.object({
  value: z.string(),
  source: z.string(),
  sensitive: z.boolean(),
  readOnly: z.boolean(),
  type: z.string(),
});
const ConfigMapSchema = z.record(z.string(), ConfigSchema);
export type ConfigMap = z.infer<typeof ConfigMapSchema>;
const NewConfigMapSchema = z.record(
  z.string(),
  z.object({
    value: z.union([z.string(), z.number(), z.undefined(), z.null()]),
  }),
);
export type NewConfigMap = z.infer<typeof NewConfigMapSchema>;
const TopicSchema = z.object({
  id: z.string(),
  type: z.literal("topics"),
  attributes: z.object({
    name: z.string(),
    status: z.string(),
    visibility: z.string(),
    partitions: z.array(PartitionSchema).optional(),
    numPartitions: z.number().optional(),
    authorizedOperations: z.array(z.string()),
    configs: ConfigMapSchema,
    recordCount: z.number().optional(),
    totalLeaderLogBytes: z.number().optional().nullable(),
  }),
  relationships: z.object({
    consumerGroups: z.object({
      data: z.array(z.any()),
    }),
  }),
});
export const TopicResponse = z.object({
  data: TopicSchema,
});
export type Topic = z.infer<typeof TopicSchema>;
const TopicListSchema = z.object({
  id: z.string(),
  type: z.literal("topics"),
  meta: z.object({
    page: z.object({
      cursor: z.string(),
    }),
  }),
  attributes: TopicSchema.shape.attributes.pick({
    name: true,
    status: true,
    visibility: true,
    numPartitions: true,
    recordCount: true,
    totalLeaderLogBytes: true,
  }),
  relationships: z.object({
    consumerGroups: z.object({
      data: z.array(z.any()),
    }),
  }),
});
export type TopicList = z.infer<typeof TopicListSchema>;
export const TopicsResponseSchema = z.object({
  meta: z.object({
    page: z.object({
      total: z.number(),
      pageNumber: z.number().optional(),
    }),
  }),
  links: z.object({
    first: z.string().nullable(),
    prev: z.string().nullable(),
    next: z.string().nullable(),
    last: z.string().nullable(),
  }),
  data: z.array(TopicListSchema),
});
export type TopicsResponse = z.infer<typeof TopicsResponseSchema>;
const TopicCreateResponseSuccessSchema = z.object({
  data: z.object({
    id: z.string(),
  }),
});
export const TopicMutateResponseErrorSchema = z.object({
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
export const TopicCreateResponseSchema = z.union([
  TopicCreateResponseSuccessSchema,
  TopicMutateResponseErrorSchema,
]);
export type TopicMutateError = z.infer<typeof TopicMutateResponseErrorSchema>;
export type TopicCreateResponse = z.infer<typeof TopicCreateResponseSchema>;
