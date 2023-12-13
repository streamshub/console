export const cpu = (namespace: string, cluster: string) => `
  sum by (nodeId, __console_metric_name__) (
    label_replace(
      label_replace(
        rate(container_cpu_usage_seconds_total{namespace="${namespace}",pod=~"${cluster}-.+-\\\\d+"}[1m]),
        "nodeId",
        "$1",
        "pod",
        ".+-(\\\\d+)"
      ),
      "__console_metric_name__",
      "cpu_usage_seconds",
      "",
      ""
    )
  )
`;

export const memory = (namespace: string, cluster: string) => `
  sum by (nodeId, __console_metric_name__) (
    label_replace(
      label_replace(
        container_memory_usage_bytes{namespace="${namespace}",pod=~"${cluster}-.+-\\\\d+"},
        "nodeId",
        "$1",
        "pod",
        ".+-(\\\\d+)"
      ),
      "__console_metric_name__",
      "memory_usage_bytes",
      "",
      ""
    )
  )
`;

export const incomingByteRate = (namespace: string, cluster: string) => `
  sum by (__console_metric_name__) (
    label_replace(
      irate(kafka_server_brokertopicmetrics_bytesin_total{topic!="",namespace="${namespace}",pod=~"${cluster}-.+-\\\\d+",strimzi_io_kind="Kafka"}[5m]),
      "__console_metric_name__",
      "incoming_byte_rate",
      "",
      ""
    )
  )
`;

export const outgoingByteRate = (namespace: string, cluster: string) => `
  sum by (__console_metric_name__) (
    label_replace(
      irate(kafka_server_brokertopicmetrics_bytesout_total{topic!="",namespace="${namespace}",pod=~"${cluster}-.+-\\\\d+",strimzi_io_kind="Kafka"}[5m]),
      "__console_metric_name__",
      "outgoing_byte_rate",
      "",
      ""
    )
  )
`;

export const volumeCapacity = (namespace: string, cluster: string) => `
  sum by (nodeId, __console_metric_name__) (
    label_replace(
      label_replace(
        kubelet_volume_stats_capacity_bytes{namespace="${namespace}",persistentvolumeclaim=~"data(?:-\\\\d+)?-${cluster}-.+-\\\\d+"},
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
`;

export const volumeUsed = (namespace: string, cluster: string) => `
  sum by (nodeId, __console_metric_name__) (
    label_replace(
      label_replace(
        kubelet_volume_stats_used_bytes{namespace="${namespace}",persistentvolumeclaim=~"data(?:-\\\\d+)?-${cluster}-.+-\\\\d+"},
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
