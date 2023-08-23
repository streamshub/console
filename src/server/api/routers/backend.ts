import { z } from "zod";
import { env } from "~/env.mjs";
import {
  createTRPCRouter,
  protectedProcedure,
  publicProcedure,
} from "~/server/api/trpc";

export const BackendError = z.object({
  meta: z.map(z.string(), z.string()),
  id: z.string(),
  status: z.string(),
  code: z.string(),
  title: z.string(),
  detail: z.string(),
  source: z.object({
    pointer: z.string(),
    parameter: z.string(),
    header: z.string(),
  }),
});

const Node = z.object({
  id: z.bigint(),
  host: z.string(),
  port: z.bigint(),
});

const KafkaCluster = z.object({
  id: z.string(),
  name: z.string(),
  bootstrapServers: z.string(),
  controller: Node,
  nodes: z.array(Node),
  status: z.object({
    conditions: z.array(
      z.object({
        status: z.string(),
        type: z.string(),
      })
    ),
  }),
  namespace: z.string(),
  creationTimestamp: z.string(),
});

const Topic = z.object({
  id: z.string(),
  name: z.string(),
  partitions: z.union([
    BackendError,
    z.array(z.object({
      partition: z.bigint(),
      leader: Node,
      replicas: z.array(Node),
      isr: z.array(Node),
    })),
  ]),
  authorizedOperations: z.union([
    BackendError,
    z.array(z.string()),
  ]),
  configs: z.union([
    BackendError,
    z.map(z.string(), z.object({
      name: z.string(),
      value: z.string(),
      source: z.string(),
      sensitive: z.boolean(),
      readOnly: z.boolean(),
      type: z.string(),
      documentation: z.string(),
    }))
  ]),
})


export const backend = createTRPCRouter({
  listClusters: publicProcedure.query(
    async (): Promise<z.infer<typeof KafkaCluster>[]> => {
      return fetch(`${env.BACKEND_URL}/api/kafkas`)
            .then(response => {
              if (!response.ok) {
                throw new Error(response.statusText)
              }
              return response.json()
            })
            .then(listing => {
              return listing.data.map((entry: any) => {
                var id = entry.id;
                return {
                  id,
                  ...entry.attributes,
                };
              });
            })
    }
  ),

  describeCluster: publicProcedure
    .input(z.object({ id: z.string() }))
    .query(async ({ input }): Promise<z.infer<typeof KafkaCluster>> => {
      return fetch(`${env.BACKEND_URL}/api/kafkas/${input.id}`)
            .then(response => {
              if (!response.ok) {
                throw new Error(response.statusText)
              }
              return response.json()
            })
            .then(entry => {
              var id = entry.data.id;
              return {
                id,
                ...entry.data.attributes,
              };
            })
    }),

    listTopics: publicProcedure
      .input(z.object({ id: z.string() }))
      .query(async ({ input }): Promise<z.infer<typeof Topic>[]> => {
        return fetch(`${env.BACKEND_URL}/api/kafkas/${input.id}/topics?fields[topics]=name,partitions`)
              .then(response => {
                if (!response.ok) {
                  throw new Error(response.statusText)
                }
                return response.json()
              })
              .then(listing => {
                return listing.data.map((entry: any) => {
                  var id = entry.id;
                  return {
                    id,
                    ...entry.attributes,
                  };
                });
              })
      }
    ),

    describeTopic: publicProcedure
      .input(z.object({ id: z.string(), topicId: z.string(), }))
      .query(async ({ input }): Promise<z.infer<typeof Topic>> => {
        return fetch(`${env.BACKEND_URL}/api/kafkas/${input.id}/topics/${input.topicId}?fields[topics]=name,partitions,configs`)
              .then(response => {
                if (!response.ok) {
                  throw new Error(response.statusText)
                }
                return response.json()
              })
              .then(entry => {
                var id = entry.data.id;
                return {
                  id,
                  ...entry.data.attributes,
                };
              })
      }),
});
