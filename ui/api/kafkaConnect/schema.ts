import { z } from "zod";

const IncludedConnectClusterSchema = z.object({
  id: z.string(),
  type: z.literal("connects"),
  attributes: z.object({
    name: z.string(),
    namespace: z.string().nullable().optional(),
    creationTimestamp: z.string().nullable().optional(),
    commit: z.string().optional(),
    kafkaClusterId: z.string().optional(),
    version: z.string().optional(),
    replicas: z.any().nullable().optional(),
  }),
  meta: z
    .object({
      managed: z.boolean(),
    })
    .optional(),
  relationships: z
    .object({
      kafkaClusters: z.object({
        data: z.array(
          z.object({
            type: z.literal("kafkas"),
            id: z.string(),
          }),
        ),
      }),
    })
    .optional(),
});

export const ConnectorSchema = z.object({
  name: z.string(),
  type: z.enum(["source", "sink"]),
  state: z.string(),
});

export const ConnectorsSchema = z.object({
  id: z.string(),
  type: z.literal("connectors"),
  attributes: ConnectorSchema,
  relationships: z
    .object({
      connectCluster: z.object({
        data: z
          .object({
            type: z.literal("connects"),
            id: z.string(),
          })
          .nullable(),
      }),
    })
    .optional(),
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
  included: z.array(IncludedConnectClusterSchema).optional(),
});

const ConnectClusterRelationshipsSchema = z.object({
  connectors: z.object({
    data: z.array(
      z.object({
        type: z.literal("connectors"),
        id: z.string(),
      }),
    ),
  }),
});

export const ConnectClusters = z.object({
  id: z.string(),
  type: z.literal("connects"),
  meta: z.object({
    managed: z.boolean(),
    page: z
      .object({
        cursor: z.string(),
      })
      .optional(),
  }),
  attributes: z.object({
    name: z.string(),
    version: z.string(),
    replicas: z.number().nullable(),
  }),
  relationships: ConnectClusterRelationshipsSchema,
});

export const IncludedConnectorSchema = z.object({
  id: z.string(),
  type: z.literal("connectors"),
  meta: z.object({
    managed: z.boolean(),
  }),
  attributes: z.object({
    name: z.string(),
    namespace: z.string().nullable(),
    creationTimestamp: z.string().nullable(),
    type: z.enum(["source", "sink"]),
    state: z.string(),
    trace: z.string().nullable(),
    workerId: z.string(),
  }),
});

export const ConnectClustersResponseSchema = z.object({
  data: z.array(ConnectClusters),
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
  included: z.array(IncludedConnectorSchema).optional(),
});

export type Connectors = z.infer<typeof ConnectorsSchema>;

export type ConnectorsResponse = z.infer<typeof ConnectorsResponseSchema>;

export type ConnectClusters = z.infer<typeof ConnectClusters>;
export type IncludedConnector = z.infer<typeof IncludedConnectorSchema>;
export type ConnectClustersResponse = z.infer<
  typeof ConnectClustersResponseSchema
>;

export type EnrichedConnector = z.infer<typeof ConnectorsSchema> & {
  connectClusterName: string | null;
  replicas: number | null;
};
