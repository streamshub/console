import { getHeaders } from "@/api/_shared";
import { logger } from "@/utils/logger";
import { z } from "zod";

const log = logger.child({ module: "api-topics" });

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
export type BrokerConfig = z.infer<typeof ConfigSchema>;

const ConfigResponseSchema = z.object({
  data: ConfigSchema,
});

export async function getBrokerConfiguration(
  kafkaId: string,
  brokerId: number | string,
): Promise<BrokerConfig> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${kafkaId}/nodes/${brokerId}/configs`;
  log.debug({ url }, "Fetching broker configuration");
  const res = await fetch(url, {
    headers: await getHeaders(),
    cache: "no-store",
    next: { tags: [`brokers-${brokerId}`] },
  });
  const rawData = await res.json();
  log.trace(rawData, "Broker configuration response");
  const data = ConfigResponseSchema.parse(rawData);
  return data.data;
}
