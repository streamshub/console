import { z } from "zod";

const ConnectorStateSchema = z.union([
  z.literal("UNASSIGNED"),
  z.literal("RUNNING"),
  z.literal("PAUSED"),
  z.literal("STOPPED"),
  z.literal("FAILED"),
  z.literal("RESTARTING"),
]);

const ConnectorTypeSchema = z.union([
  z.literal("source"),
  z.literal("sink"),
  z.literal("source:mm"),
  z.literal("source:mm-checkpoint"),
  z.literal("source:mm-heartbeat"),
]);

export const ConnectorConfigSchema = z.record(
  z.string(),
  z.string().nullable().optional(),
);

export const ConnectorsDataSchema = z.object({
  name: z.string(),
  namespace: z.string().nullable().optional(),
  creationTimestamp: z.string().nullable().optional(),
  type: ConnectorTypeSchema,
  state: ConnectorStateSchema,
  trace: z.string().nullable().optional(),
  workerId: z.string().optional(),
  topics: z.array(z.string()).optional(),
  config: ConnectorConfigSchema.optional(),
});

const PluginSchema = z.object({
  class: z.string(),
  type: ConnectorTypeSchema,
  version: z.string(),
});

export const ConnectClusterDataSchema = z.object({
  name: z.string(),
  namespace: z.string().nullable().optional(),
  creationTimestamp: z.string().nullable().optional(),
  commit: z.string().optional(),
  kafkaClusterId: z.string().optional(),
  version: z.string().optional(),
  replicas: z.any().nullable().optional(),
  plugins: z.array(PluginSchema).optional(),
});

const ConnectClusterSchema = z.object({
  id: z.string(),
  type: z.literal("connects"),
  attributes: ConnectClusterDataSchema,
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

export const ConnectorsSchema = z.object({
  id: z.string(),
  type: z.literal("connectors"),
  attributes: ConnectorsDataSchema,
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
      managed: z.boolean().optional(),
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
  included: z.array(ConnectClusterSchema).optional(),
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

export const ConnectClusterDetailResponseSchema = z.object({
  data: z.object({
    id: z.string(),
    type: z.literal("connects"),
    meta: z
      .object({
        managed: z.boolean(),
      })
      .optional(),
    attributes: ConnectClusterDataSchema,
    relationships: z.object({
      connectors: z.object({
        data: z.array(
          z.object({
            type: z.literal("connectors"),
            id: z.string(),
          }),
        ),
      }),
    }),
  }),

  included: z
    .array(
      z.object({
        id: z.string(),
        type: z.literal("connectors"),
        meta: z
          .object({
            managed: z.boolean(),
          })
          .optional(),
        attributes: ConnectorsDataSchema,
      }),
    )
    .optional(),
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
  attributes: ConnectClusterDataSchema,
  relationships: ConnectClusterRelationshipsSchema,
});

export const ConnectorSchema = z.object({
  id: z.string(),
  type: z.literal("connectors"),
  meta: z.object({
    managed: z.boolean(),
  }),
  relationships: z.record(z.string(), z.unknown()).optional(),
  attributes: ConnectorsDataSchema,
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
  included: z.array(ConnectorSchema).optional(),
});

const connectorTaskSchema = z.object({
  id: z.string(),
  type: z.literal("connectorTasks"),
  attributes: z.object({
    taskId: z.number(),
    config: ConnectorConfigSchema.optional(),
    state: ConnectorStateSchema,
    workerId: z.string(),
  }),
});

export const connectorDetailSchema = z.object({
  data: z.object({
    id: z.string(),
    type: z.literal("connectors"),
    meta: z.object({
      managed: z.boolean(),
    }),
    attributes: ConnectorsDataSchema,
    relationships: z.object({
      connectCluster: z.object({
        data: z.object({
          type: z.literal("connects"),
          id: z.string(),
        }),
      }),
      tasks: z.object({
        data: z.array(
          z.object({
            type: z.literal("connectorTasks"),
            id: z.string(),
          }),
        ),
      }),
    }),
  }),
  included: z.array(z.union([ConnectClusterSchema, connectorTaskSchema])),
});

export type Connectors = z.infer<typeof ConnectorsSchema>;

export type ConnectorsResponse = z.infer<typeof ConnectorsResponseSchema>;

export type ConnectClusters = z.infer<typeof ConnectClusters>;
export type IncludedConnector = z.infer<typeof ConnectorSchema>;
export type ConnectClustersResponse = z.infer<
  typeof ConnectClustersResponseSchema
>;

export type EnrichedConnector = z.infer<typeof ConnectorsSchema> & {
  connectClusterId: string | null;
  connectClusterName: string | null;
  replicas: number | null;
};

export type ConnectorState = z.infer<typeof ConnectorStateSchema>;

export type ConnectCluster = z.infer<typeof ConnectClusterDetailResponseSchema>;

export type plugins = z.infer<typeof PluginSchema>;

export type ConnectorCluster = z.infer<typeof connectorDetailSchema>;

export type ConnectorTask = z.infer<typeof connectorTaskSchema>;

export type ConnectorConfig = z.infer<typeof ConnectorConfigSchema>;

export type ConnectorType = z.infer<typeof ConnectorTypeSchema>;
