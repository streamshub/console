"use server";
import { getHeaders } from "@/api/api";
import {
  ClusterDetail,
  ClusterKpis,
  ClusterKpisSchema,
  ClusterList,
  ClusterResponse,
  ClustersResponseSchema,
  MetricRange,
  MetricRangeSchema,
} from "@/api/kafka/schema";
import { logger } from "@/utils/logger";
import groupBy from "lodash.groupby";
import { PrometheusDriver } from "prometheus-query";
import * as clusterPromql from "./cluster.promql";
import { values } from "./kpi.promql";
import * as topicPromql from "./topic.promql";

export type ClusterMetric = keyof typeof clusterPromql;
export type TopicMetric = keyof typeof topicPromql;

const prom = process.env.CONSOLE_METRICS_PROMETHEUS_URL
  ? new PrometheusDriver({
      endpoint: process.env.CONSOLE_METRICS_PROMETHEUS_URL,
    })
  : undefined;

const log = logger.child({ module: "kafka-api" });

export async function getKafkaClusters(): Promise<ClusterList[]> {
  const sp = new URLSearchParams({
    "fields[kafkas]": "name,namespace,kafkaVersion",
    sort: "name",
  });
  const kafkaClustersQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas?${kafkaClustersQuery}`;
  try {
    const res = await fetch(url, {
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      next: {
        revalidate: 30,
      },
    });
    const rawData = await res.json();
    log.trace(rawData, "getKafkaClusters response");
    return ClustersResponseSchema.parse(rawData).data;
  } catch (err) {
    log.error(err, "getKafkaClusters");
    throw new Error("getKafkaClusters: couldn't connect with backend");
  }
}

export async function getKafkaCluster(
  clusterId: string,
): Promise<ClusterDetail | null> {
  const sp = new URLSearchParams({
    "fields[kafkas]":
      "name,namespace,creationTimestamp,status,kafkaVersion,nodes,controller,authorizedOperations,listeners,conditions,nodePools,cruiseControlEnabled",
  });
  const kafkaClusterQuery = sp.toString();
  const url = `${process.env.BACKEND_URL}/api/kafkas/${clusterId}?${kafkaClusterQuery}`;
  try {
    const res = await fetch(url, {
      headers: await getHeaders(),
    });
    if (res.status === 200) {
      const rawData = await res.json();
      log.trace(rawData, "getKafkaCluster response");
      return ClusterResponse.parse(rawData).data;
    }
    return null;
  } catch (err) {
    log.error({ err, clusterId }, "getKafkaCluster");
    throw new Error("getKafkaCluster: couldn't connect with backend");
  }
}

export async function getKafkaClusterKpis(
  clusterId: string,
): Promise<{ cluster: ClusterDetail; kpis: ClusterKpis | null } | null> {
  const cluster = await getKafkaCluster(clusterId);

  if (!cluster) {
    return null;
  }

  if (!prom || !cluster.attributes.namespace) {
    log.warn({ clusterId }, "getKafkaClusterKpis Prometheus unavailable");
    return { cluster, kpis: null };
  }

  try {
    const valuesRes = await prom.instantQuery(
      values(
        cluster.attributes.namespace,
        cluster.attributes.name,
        cluster.attributes.controller.id,
        cluster.attributes.nodePools?.join("|") ?? "",
      ),
    );

    log.debug("getKafkaClusterKpis response: " + JSON.stringify(valuesRes));

    /*
    Prometheus returns the data unaggregated. Eg.

    [
      {
        "metric": {
          "labels": {
            "__console_metric_name__": "broker_state",
            "nodeId": "2"
          }
        },
        "value": {
          "time": "2023-12-12T16:00:53.381Z",
          "value": 3
         }
      },
      ...
    ]

    We start by flattening the labels, and then group by metric name
     */
    const groupedMetrics = groupBy(
      valuesRes.result.map((serie) => ({
        metric: serie.metric.labels.__console_metric_name__,
        nodeId: serie.metric.labels.nodeId,
        time: serie.value.time,
        value: serie.value.value,
      })),
      (v) => v.metric,
    );

    /*
    Now we want to transform the data in something easier to work with in the UI.

    Some are totals, in an array form with a single entry; we just need the number. These will look like a metric:value
    mapping.

    Some KPIs are provided split by broker id. Of these, some are counts (identified by the string `_count` in the
    metric name), and some are other infos. Both will be grouped by nodeId.
    The `_count` metrics will have a value with two properties, `byNode` and `total`. `byNode` will hold the grouping. `total` will
    have the sum of all the counts.
    Other metrics will look like a metric:[node:value] mapping.

    Expected result:
    {
      "broker_state": {
        "0": 3,
        "1": 3,
        "2": 3
      },
      "total_topics": 5,
      "total_partitions": 55,
      "underreplicated_topics": 0,
      "replica_count": {
        "byNode": {
          "0": 57,
          "1": 54,
          "2": 54
        },
        "total": 165
      },
      "leader_count": {
        "byNode": {
          "0": 19,
          "1": 18,
          "2": 18
        },
        "total": 55
      }
    }
     */
    const kpis = Object.fromEntries(
      Object.entries(groupedMetrics).map(([metric, value]) => {
        const total = value.reduce((acc, v) => acc + v.value, 0);
        if (value.find((v) => v.nodeId)) {
          const byNode = Object.fromEntries(
            value.map(({ nodeId, value }) =>
              nodeId ? [nodeId, value] : ["value", value],
            ),
          );
          return metric.includes("_count") || metric.includes("bytes")
            ? [
                metric,
                {
                  byNode,
                  total,
                },
              ]
            : [metric, byNode];
        } else {
          return [metric, total];
        }
      }),
    );
    log.debug({ kpis, clusterId }, "getKafkaClusterKpis");
    return {
      cluster,
      kpis: ClusterKpisSchema.parse(kpis),
    };
  } catch (err) {
    log.error({ err, clusterId }, "getKafkaClusterKpis");
    return {
      cluster,
      kpis: null,
    };
  }
}

export async function getKafkaClusterMetrics(
  clusterId: string,
  metrics: Array<ClusterMetric>,
): Promise<{
  cluster: ClusterDetail;
  ranges: Record<ClusterMetric, MetricRange> | null;
} | null> {
  async function getRangeByNodeId(
    namespace: string,
    name: string,
    nodePools: string,
    metric: ClusterMetric,
  ) {
    const start = new Date().getTime() - 1 * 60 * 60 * 1000;
    const end = new Date();
    const step = 60 * 1;
    const seriesRes = await prom!.rangeQuery(
      clusterPromql[metric](namespace, name, nodePools),
      start,
      end,
      step,
    );
    const serieByNode = Object.fromEntries(
      seriesRes.result.map((serie) => [
        serie.metric.labels.nodeId,
        Object.fromEntries(
          serie.values.map((v: any) => [new Date(v.time).getTime(), v.value]),
        ),
      ]),
    );
    return [metric, MetricRangeSchema.parse(serieByNode)];
  }

  const cluster = await getKafkaCluster(clusterId);

  if (!cluster) {
    return null;
  }

  if (!prom || !cluster.attributes.namespace) {
    log.warn({ clusterId }, "getKafkaClusterKpis Prometheus unavailable");
    return { cluster, ranges: null };
  }

  try {
    const rangesRes = Object.fromEntries(
      await Promise.all(
        metrics.map((m) =>
          getRangeByNodeId(
            cluster.attributes.namespace!,
            cluster.attributes.name,
            cluster.attributes.nodePools?.join("|") ?? "",
            m,
          ),
        ),
      ),
    );
    log.debug(
      { ranges: rangesRes, clusterId, metric: metrics },
      "getKafkaClusterMetric",
    );
    return {
      cluster,
      ranges: rangesRes,
    };
  } catch (err) {
    log.error({ err, clusterId, metric: metrics }, "getKafkaClusterMetric");
    return {
      cluster,
      ranges: null,
    };
  }
}

export async function getKafkaTopicMetrics(
  clusterId: string,
  metrics: Array<TopicMetric>,
): Promise<{
  cluster: ClusterDetail;
  ranges: Record<TopicMetric, MetricRange> | null;
} | null> {
  async function getRangeByNodeId(
    namespace: string,
    name: string,
    nodePools: string,
    metric: TopicMetric,
  ) {
    const start = new Date().getTime() - 1 * 60 * 60 * 1000;
    const end = new Date();
    const step = 60 * 1;
    const seriesRes = await prom!.rangeQuery(
      topicPromql[metric](namespace, name, nodePools),
      start,
      end,
      step,
    );
    const serieByNode = Object.fromEntries(
      seriesRes.result.map((serie) => [
        "all topics",
        Object.fromEntries(
          serie.values.map((v: any) => [new Date(v.time).getTime(), v.value]),
        ),
      ]),
    );
    return [metric, MetricRangeSchema.parse(serieByNode)];
  }

  const cluster = await getKafkaCluster(clusterId);

  if (!cluster) {
    return null;
  }

  try {
    if (!prom || !cluster.attributes.namespace) {
      log.warn({ clusterId }, "getKafkaClusterKpis Prometheus unavailable");
      return { cluster, ranges: null };
    }

    const rangesRes = Object.fromEntries(
      await Promise.all(
        metrics.map((m) =>
          getRangeByNodeId(
            cluster.attributes.namespace!,
            cluster.attributes.name,
            cluster.attributes.nodePools?.join("|") ?? "",
            m,
          ),
        ),
      ),
    );
    log.debug(
      { ranges: rangesRes, clusterId, metric: metrics },
      "getKafkaTopicMetrics",
    );
    return {
      cluster,
      ranges: rangesRes,
    };
  } catch (err) {
    log.error({ err, clusterId, metric: metrics }, "getKafkaTopicMetrics");
    return {
      cluster,
      ranges: null,
    };
  }
}
