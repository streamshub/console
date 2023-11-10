import { z } from "zod";

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
  }),
});
export const MessageApiResponse = z.object({
  meta: z.object({}).nullable().optional(),
  data: z.array(MessageSchema),
});
export type Message = z.infer<typeof MessageSchema>;
