import { getKafkaCluster } from "@/api/kafka/actions";
import { createTopic } from "@/api/topics/actions";
import { NewConfigMap } from "@/api/topics/schema";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { CreateTopic } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/create/CreateTopic";
import { redirect } from "@/i18n/routing";
import { clientConfig as config } from "@/utils/config";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export default async function CreateTopicPage(
  props: {
    params: Promise<KafkaParams>;
  }
) {
  const params = await props.params;

  const {
    kafkaId
  } = params;

  const isReadOnly = (await config()).readOnly;

  if (isReadOnly) {
    redirect(`/kafka/${kafkaId}/topics`);
    return;
  }

  const response = (await getKafkaCluster(kafkaId));

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const cluster = response.payload!;

  async function onSave(
    name: string,
    partitions: number,
    replicas: number,
    options: NewConfigMap,
    validateOnly: boolean,
  ) {
    "use server";
    try {
      return await createTopic(
        kafkaId,
        name,
        partitions,
        replicas,
        options,
        validateOnly,
      );
    } catch (e: unknown) {
      return Promise.reject("Unknown error");
    }
  }

  const combinedStatuses = cluster?.relationships.nodes?.meta?.summary?.statuses?.combined || {};
  const nodeCount = Object.values(combinedStatuses).reduce((sum, count) => sum + count, 0);

  return (
    <CreateTopic
      kafkaId={kafkaId}
      maxReplicas={nodeCount}
      initialOptions={tempOptions}
      onSave={onSave}
    />
  );
}

const tempOptions = {
  "compression.type": {
    value: "producer",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "STRING",
  },
  "leader.replication.throttled.replicas": {
    value: "",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LIST",
  },
  "message.downconversion.enable": {
    value: "true",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "BOOLEAN",
  },
  "min.insync.replicas": {
    value: "1",
    source: "STATIC_BROKER_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "INT",
  },
  "segment.jitter.ms": {
    value: "0",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LONG",
  },
  "cleanup.policy": {
    value: "delete",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LIST",
  },
  "flush.ms": {
    value: "9223372036854775807",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LONG",
  },
  "follower.replication.throttled.replicas": {
    value: "",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LIST",
  },
  "retention.ms": {
    value: "604800000",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LONG",
  },
  "segment.bytes": {
    value: "1073741824",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "INT",
  },
  "flush.messages": {
    value: "9223372036854775807",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LONG",
  },
  "message.format.version": {
    value: "3.0-IV1",
    source: "STATIC_BROKER_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "STRING",
  },
  "file.delete.delay.ms": {
    value: "60000",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LONG",
  },
  "max.compaction.lag.ms": {
    value: "9223372036854775807",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LONG",
  },
  "max.message.bytes": {
    value: "1048588",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "INT",
  },
  "min.compaction.lag.ms": {
    value: "0",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LONG",
  },
  "message.timestamp.type": {
    value: "CreateTime",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "STRING",
  },
  preallocate: {
    value: "false",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "BOOLEAN",
  },
  "index.interval.bytes": {
    value: "4096",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "INT",
  },
  "min.cleanable.dirty.ratio": {
    value: "0.5",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "DOUBLE",
  },
  "unclean.leader.election.enable": {
    value: "false",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "BOOLEAN",
  },
  "delete.retention.ms": {
    value: "86400000",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LONG",
  },
  "retention.bytes": {
    value: "-1",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LONG",
  },
  "segment.ms": {
    value: "604800000",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LONG",
  },
  "message.timestamp.difference.max.ms": {
    value: "9223372036854775807",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "LONG",
  },
  "segment.index.bytes": {
    value: "10485760",
    source: "DEFAULT_CONFIG",
    sensitive: false,
    readOnly: false,
    type: "INT",
  },
};
