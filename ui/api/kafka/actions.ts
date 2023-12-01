"use server";
import { getHeaders } from "@/api/api";
import {
  ClusterDetail,
  ClusterKpis,
  ClusterKpisSchema,
  ClusterList,
  ClusterMetricRange,
  ClusterMetricRangeSchema,
  ClusterResponse,
  ClustersResponseSchema,
} from "@/api/kafka/schema";
import { logger } from "@/utils/logger";
import groupBy from "lodash.groupby";
import { PrometheusDriver } from "prometheus-query";
import * as ranges from "./ranges.promql";
import { values } from "./values.promql";

export type Range = keyof typeof ranges;

const prom = new PrometheusDriver({
  endpoint: process.env.CONSOLE_METRICS_PROMETHEUS_URL,
});

const log = logger.child({ module: "kafka-api" });

export async function getKafkaClusters(): Promise<ClusterList[]> {
  const url = `${process.env.BACKEND_URL}/api/kafkas?fields%5Bkafkas%5D=name,namespace,bootstrapServers`;
  try {
    const res = await fetch(url, {
      headers: await getHeaders(),
    });
    const rawData = await res.json();
    return ClustersResponseSchema.parse(rawData).data;
  } catch (err) {
    log.error(err, "getKafkaClusters");
    return [];
  }
}

export async function getKafkaCluster(
  clusterId: string,
): Promise<ClusterDetail | null> {
  const url = `${process.env.BACKEND_URL}/api/kafkas/${clusterId}/?fields%5Bkafkas%5D=name,namespace,creationTimestamp,status,kafkaVersion,nodes,controller,authorizedOperations,bootstrapServers,authType`;
  try {
    const res = await fetch(url, {
      headers: await getHeaders(),
      cache: "force-cache",
    });
    const rawData = await res.json();
    log.debug(rawData, "getKafkaCluster response");
    return ClusterResponse.parse(rawData).data;
  } catch (err) {
    log.error({ err, clusterId }, "getKafkaCluster");
    return null;
  }
}

export async function getKafkaClusterKpis(
  clusterId: string,
): Promise<{ cluster: ClusterDetail; kpis: ClusterKpis } | null> {
  try {
    const cluster = await getKafkaCluster(clusterId);
    if (!cluster) {
      return null;
    }

    const valuesRes = await prom.instantQuery(
      values(
        cluster.attributes.namespace,
        cluster.attributes.name,
        cluster.attributes.controller.id,
      ),
    );

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
          return metric.includes("_count")
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
    return null;
  }
}

export async function getKafkaClusterMetrics(
  clusterId: string,
  metrics: Array<Range>,
): Promise<{
  cluster: ClusterDetail;
  ranges: Record<Range, ClusterMetricRange>;
} | null> {
  async function getRange(namespace: string, name: string, metric: Range) {
    const start = new Date().getTime() - 1 * 60 * 60 * 1000;
    const end = new Date();
    const step = 60 * 2;
    const seriesRes = await prom.rangeQuery(
      ranges[metric](namespace, name),
      start,
      end,
      step,
    );
    const range = Object.fromEntries(
      seriesRes.result.flatMap((serie) =>
        serie.values.map((v: any) => [new Date(v.time).getTime(), v.value]),
      ),
    );
    return [metric, ClusterMetricRangeSchema.parse(range)];
  }

  try {
    const cluster = await getKafkaCluster(clusterId);
    if (!cluster) {
      return null;
    }

    const rangesRes = Object.fromEntries(
      await Promise.all(
        metrics.map((m) =>
          getRange(cluster.attributes.namespace, cluster.attributes.name, m),
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
    return null;
  }
}
