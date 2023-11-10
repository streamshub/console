import { z } from "zod";

const ConfigSchema = z.object({
  id: z.string().optional(),
  type: z.string(),
  meta: z.record(z.any()).optional(),
  attributes: z.record(
    z.object({
      value: z.string().optional(),
      source: z.string().readonly(),
      sensitive: z.boolean().readonly(),
      readOnly: z.boolean().readonly(),
      type: z.string().readonly(),
      documentation: z.string().readonly().optional(),
    }),
  ),
});
export type NodeConfig = z.infer<typeof ConfigSchema>;
export const ConfigResponseSchema = z.object({
  data: ConfigSchema,
});
