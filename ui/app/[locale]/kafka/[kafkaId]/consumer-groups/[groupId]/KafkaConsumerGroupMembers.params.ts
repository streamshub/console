import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";

export type KafkaConsumerGroupMembersParams = KafkaParams & { groupId: string };
