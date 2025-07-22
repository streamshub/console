import { z } from "zod";

export const ConnectorSchema = z.object({
  name: z.string(),
  namespace: z.string().nullable(),
  creationTimestamp: z.string().nullable(),
  type: z.enum(["source", "sink"]),
  state: z.string(),
  trace: z.string().nullable(),
  workerId: z.string(),
});

export const ConnectorsSchema = z.object({
  id: z.string(),
  type: z.literal("connectors"),
  attributes: ConnectorSchema,
  meta: z
    .object({
      page: z.object({
        cursor: z.string(),
      }),
    })
    .optional(),
});

export const ConnectorsResponseSchema = z.object({
  data: z.array(ConnectorsSchema),
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
});

export type Connectors = z.infer<typeof ConnectorsSchema>;
