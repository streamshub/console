export const incomingByteRate = (namespace: string, cluster: string) => `
  sum by (__console_metric_name__) (
    label_replace(
      irate(kafka_server_brokertopicmetrics_bytesin_total{topic!="",namespace="${namespace}",pod=~"${cluster}-kafka-\\\\d+",strimzi_io_kind="Kafka"}[5m]),
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
      irate(kafka_server_brokertopicmetrics_bytesout_total{topic!="",namespace="${namespace}",pod=~"${cluster}-kafka-\\\\d+",strimzi_io_kind="Kafka"}[5m]),
      "__console_metric_name__",
      "outgoing_byte_rate",
      "",
      ""
    )
  )
`;
