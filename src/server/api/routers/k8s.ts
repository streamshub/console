import { ApisApi, CustomObjectsApi, KubeConfig } from "@kubernetes/client-node";
import { z } from "zod";
import { kafkaCRD } from "~/server/api/routers/kafkaCRD";
import {
  createTRPCRouter,
  protectedProcedure,
  publicProcedure,
} from "~/server/api/trpc";

const Metadata = z.object({
  name: z.string(),
  namespace: z.string(),
  uid: z.string(),
  creationTimestamp: z.string(),
});

const Kafka = Metadata.extend(kafkaCRD.shape);

const kc = new KubeConfig();
kc.loadFromDefault();

const apisApi = kc.makeApiClient(ApisApi);
const coApi = kc.makeApiClient(CustomObjectsApi);

async function getStrimziApiInfo() {
  const apis = await apisApi.getAPIVersions();
  const strimziApiInfo = apis.body.groups.find(
    (g) => g.name === "kafka.strimzi.io"
  );
  if (strimziApiInfo === undefined || strimziApiInfo.versions.length === 0) {
    throw new Error("Strimzi non installed");
  }
  return {
    group: strimziApiInfo.name,
    version: strimziApiInfo.versions[0]!.version,
    plural: "kafkas",
  };
}

export const k8s = createTRPCRouter({
  hello: publicProcedure
    .input(z.object({ text: z.string() }))
    .query(({ input }) => {
      return {
        greeting: `Hello ${input.text}`,
      };
    }),

  getSecretMessage: protectedProcedure.query(() => {
    return "you can now see this secret message!";
  }),

  getKafkaClusters: publicProcedure.query(
    async (): Promise<z.infer<typeof Kafka>[]> => {
      const { group, version, plural } = await getStrimziApiInfo();

      const res = await coApi.listClusterCustomObject(group, version, plural);
      const kafkas = (
        res.body as { items: Record<string, unknown>[] }
      ).items.map((k) => {
        const metadata = Metadata.parse(k.metadata);
        const crd = kafkaCRD.parse(k);
        return {
          ...metadata,
          ...crd,
        };
      });
      return kafkas;
    }
  ),

  getKafkaCluster: publicProcedure
    .input(z.object({ name: z.string(), namespace: z.string() }))
    .query(async ({ input }): Promise<z.infer<typeof Kafka>> => {
      const { group, version, plural } = await getStrimziApiInfo();

      const res = await coApi.getNamespacedCustomObject(
        group,
        version,
        input.namespace,
        plural,
        input.name
      );
      const raw = res.body as Record<string, unknown>;

      const metadata = Metadata.parse(raw.metadata);
      const crd = kafkaCRD.parse(raw);
      return {
        ...metadata,
        ...crd,
      };
    }),
});
