import { z } from "zod";

const RelatedSchema = z.object({
  meta: z.object({
    artifactType: z.string().optional(),
    name: z.string().optional(),
  }).nullable().optional(),
  links: z.object({
    content: z.string(),
  }).nullable().optional(),
  data: z.object({
    type: z.literal("schemas"),
    id: z.string(),
  }),
});

export const MessageSchema = z.object({
  type: z.literal("records"),
  attributes: z.object({
    partition: z.number(),
    offset: z.number(),
    timestamp: z.string(),
    timestampType: z.string(),
    headers: z.record(z.any()),
    key: z.string().nullable(),
    value: z.string().nullable(),
    size: z.number().optional(),
  }),
  relationships: z.object({
    keySchema: RelatedSchema.optional().nullable(),
    valueSchema: RelatedSchema.optional().nullable(),
  }),
});

export const MessageApiResponse = z.object({
  meta: z.object({}).nullable().optional(),
  data: z.array(MessageSchema),
});

export type Message = z.infer<typeof MessageSchema>;
