export const cpu = (namespace: string, cluster: string) => `
  sum by (nodeId, __console_metric_name__) (
    label_replace(
      label_replace(
        rate(container_cpu_usage_seconds_total{namespace="${namespace}",pod=~"${cluster}-.+-\\\\d+",container="kafka"}[1m]),
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
        container_memory_usage_bytes{namespace="${namespace}",pod=~"${cluster}-.+-\\\\d+",container="kafka"},
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

export const volumeCapacity = (namespace: string, cluster: string) => `
  sum by (nodeId, __console_metric_name__) (
    label_replace(
      label_replace(
        kubelet_volume_stats_capacity_bytes{namespace="${namespace}",persistentvolumeclaim=~"data(?:-\\\\d+)?-${cluster}-kafka-\\\\d+"},
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
        kubelet_volume_stats_used_bytes{namespace="${namespace}",persistentvolumeclaim=~"data(?:-\\\\d+)?-${cluster}-kafka-\\\\d+"},
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

