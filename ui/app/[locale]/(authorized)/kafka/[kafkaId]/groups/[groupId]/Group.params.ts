import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";

export type GroupParams = KafkaParams & { groupId: string };
