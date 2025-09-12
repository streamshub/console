import { z } from "zod";

export const MetadataSchema = z.object({
  id: z.string(),
  type: z.literal("metadata"),
  attributes: z.object({
    version: z.string(),
    platform: z.string(),
  }),
});

export type MetadataResponse = z.infer<typeof MetadataSchema>;
