import { BackendError } from "@/api/api";
import { z } from "zod";

export const describeTopicsQuery = encodeURI(
  "fields[topics]=,name,internal,partitions,authorizedOperations,configs,recordCount,totalLeaderLogBytes",
);
const OffsetSchema = z.object({
  offset: z.number().optional(),
  timestamp: z.string().optional(),
  leaderEpoch: z.number().optional(),
});
const PartitionSchema = z.object({
  partition: z.number(),
  leaderId: z.number(),
  replicas: z.array(
    z.object({
      nodeId: z.number(),
      nodeRack: z.string().optional(),
      inSync: z.boolean(),
      localStorage: BackendError.or(
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
    internal: z.boolean(),
    partitions: z.array(PartitionSchema),
    authorizedOperations: z.array(z.string()),
    configs: ConfigMapSchema,
    recordCount: z.number().optional(),
    totalLeaderLogBytes: z.number().optional(),
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
    internal: true,
    partitions: true,
    recordCount: true,
    totalLeaderLogBytes: true,
  }),
});
export type TopicList = z.infer<typeof TopicListSchema>;
export const TopicsResponse = z.object({
  meta: z.object({
    page: z.object({
      total: z.number(),
      pageNumber: z.number(),
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
export type TopicsResponseList = z.infer<typeof TopicsResponse>;
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
