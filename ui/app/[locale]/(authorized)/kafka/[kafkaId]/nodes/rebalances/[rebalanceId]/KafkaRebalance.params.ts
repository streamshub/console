import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";

export type KafkaRebalanceParams = KafkaParams & { rebalanceId: string };
