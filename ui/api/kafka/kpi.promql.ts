export const values = (
  namespace: string,
  cluster: string,
  controller: number,
  nodePools: string,
) => `
sum by (__console_metric_name__, nodeId) (
  label_replace(
    label_replace(
      kafka_server_kafkaserver_brokerstate{namespace="${namespace}",pod=~"${cluster}-.+-\\\\d+",strimzi_io_kind="Kafka"} > 0,
      "nodeId",
      "$1",
      "pod",
      ".+-(\\\\d+)"
    ),
    "__console_metric_name__",
    "broker_state",
    "",
    ""
  )
)

or

sum by (__console_metric_name__) (
  label_replace(
    kafka_controller_kafkacontroller_globaltopiccount{namespace="${namespace}",pod=~"${cluster}-.+-${controller}",strimzi_io_kind="Kafka"} > 0,
    "__console_metric_name__",
    "total_topics",
    "",
    ""
  )
)

or

sum by (__console_metric_name__) (
  label_replace(
    kafka_controller_kafkacontroller_globalpartitioncount{namespace="${namespace}",pod=~"${cluster}-.+-${controller}",strimzi_io_kind="Kafka"} > 0,
    "__console_metric_name__",
    "total_partitions",
    "",
    ""
  )
)

or

label_replace(
  (
    count(
      sum by (topic) (
        kafka_cluster_partition_underreplicated{namespace="${namespace}",pod=~"${cluster}-.+-\\\\d+",strimzi_io_kind="Kafka"} > 0
      )
    )
    OR on() vector(0)
  ),
  "__console_metric_name__",
  "underreplicated_topics",
  "",
  ""
)

or

sum by (__console_metric_name__, nodeId) (
  label_replace(
    label_replace(
      kafka_server_replicamanager_partitioncount{namespace="${namespace}",pod=~"${cluster}-.+-\\\\d+",strimzi_io_kind="Kafka"} > 0,
      "nodeId",
      "$1",
      "pod",
      ".+-(\\\\d+)"
    ),
    "__console_metric_name__",
    "replica_count",
    "",
    ""
  )
)

or

sum by (__console_metric_name__, nodeId) (
  label_replace(
    label_replace(
      kafka_server_replicamanager_leadercount{namespace="${namespace}",pod=~"${cluster}-.+-\\\\d+",strimzi_io_kind="Kafka"} > 0,
      "nodeId",
      "$1",
      "pod",
      ".+-(\\\\d+)"
    ),
    "__console_metric_name__",
    "leader_count",
    "",
    ""
  )
)

or

sum by (__console_metric_name__, nodeId) (
  label_replace(
    label_replace(
      kubelet_volume_stats_capacity_bytes{namespace="${namespace}",persistentvolumeclaim=~"data(?:-\\\\d+)?-${cluster}-(kafka|${nodePools})-\\\\d+"},
      "nodeId",
      "$1",
      "persistentvolumeclaim",
      ".+-(\\\\d+)"
    ),
    "__console_metric_name__",
    "volume_stats_capacity_bytes",
    "",
    ""
  )
)

or

sum by (__console_metric_name__, nodeId) (
  label_replace(
    label_replace(
      kubelet_volume_stats_used_bytes{namespace="${namespace}",persistentvolumeclaim=~"data(?:-\\\\d+)?-${cluster}-(kafka|${nodePools})-\\\\d+"},
      "nodeId",
      "$1",
      "persistentvolumeclaim",
      ".+-(\\\\d+)"
    ),
    "__console_metric_name__",
    "volume_stats_used_bytes",
    "",
    ""
  )
)
`;
