import { z } from "zod";

export const AuthorizationSchema = z.object({
  type: z.string(),
  resourceName: z.string().nullable().optional(),
  patternType: z.string().nullable().optional(),
  host: z.string().nullable().optional(),
  operations: z.array(z.string()),
  permissionType: z.string(),
});

export const KafkaUserAttributesSchema = z.object({
  name: z.string(),
  namespace: z.string().nullable().optional(),
  creationTimestamp: z.string().nullable().optional(),
  username: z.string(),
  authenticationType: z.string(),
  authorization: z
    .object({
      accessControls: z.array(AuthorizationSchema),
    })
    .nullable()
    .optional(),
});

export const KafkaUserSchema = z.object({
  id: z.string(),
  type: z.literal("kafkaUsers"),
  meta: z.object({
    privileges: z.array(z.string()).optional(),
  }),
  attributes: KafkaUserAttributesSchema,
  relationships: z.record(z.string(), z.unknown()).optional(),
});

export const KafkaUserResponseSchema = z.object({
  data: KafkaUserSchema,
});

export const KafkaUsersResponseSchema = z.object({
  data: z.array(KafkaUserSchema),
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
});

export type KafkaUser = z.infer<typeof KafkaUserSchema>;
export type KafkaUserAttributes = z.infer<typeof KafkaUserAttributesSchema>;
export type KafkaUserResponse = z.infer<typeof KafkaUserResponseSchema>;
export type KafkaUsersResponse = z.infer<typeof KafkaUsersResponseSchema>;
export type Authorization = z.infer<typeof AuthorizationSchema>;
