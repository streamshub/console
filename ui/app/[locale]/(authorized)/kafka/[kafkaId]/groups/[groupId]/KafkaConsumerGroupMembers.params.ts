import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";

export type KafkaConsumerGroupMembersParams = KafkaParams & { groupId: string };
