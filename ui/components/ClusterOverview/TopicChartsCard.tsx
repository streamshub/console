"use client";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Flex,
  Title,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { ChartIncomingOutgoing } from "./components/ChartIncomingOutgoing";
import { ChartSkeletonLoader } from "./components/ChartSkeletonLoader";
import { useTranslations } from "next-intl";
import { FilterByTopic } from "./components/FilterByTopic";
import { useEffect, useMemo, useState } from "react";
import { gettopicMetrics } from "@/api/topics/actions";

function timeSeriesMetrics(
  ranges: Record<string, { range: string[][]; nodeId?: string }[]> | undefined,
  rangeName: string,
): TimeSeriesMetrics {
  let series: TimeSeriesMetrics = {};

  if (ranges) {
    Object.values(ranges[rangeName] ?? {}).forEach((r) => {
      series = r.range.reduce(
        (a, v) => ({ ...a, [v[0]]: parseFloat(v[1]) }),
        series,
      );
    });
  }

  return series;
}

type TopicOption = { id: string; name: string };

type TopicChartsCardProps = {
  incoming: TimeSeriesMetrics;
  outgoing: TimeSeriesMetrics;
  topicList: TopicOption[];
  kafkaId: string | undefined;
};

export function TopicChartsCard({
  isLoading,
  incoming,
  outgoing,
  isVirtualKafkaCluster,
  topicList,
  kafkaId,
}:
  | ({
      isLoading: false;
      isVirtualKafkaCluster: boolean;
    } & TopicChartsCardProps)
  | ({
      isLoading: true;
      isVirtualKafkaCluster?: boolean;
    } & Partial<{ [key in keyof TopicChartsCardProps]?: undefined }>)) {
  const t = useTranslations();

  const [selectedTopic, setSelectedTopic] = useState<string | undefined>();

  const selectedTopicName = topicList?.find(
    (t) => t.id === selectedTopic,
  )?.name;

  const [topicSpecificMetrics, setTopicSpecificMetrics] = useState<{
    incoming: TimeSeriesMetrics;
    outgoing: TimeSeriesMetrics;
  } | null>(null);

  const [isFetching, setIsFetching] = useState(false);

  useEffect(() => {
    async function fetchSpecificMetrics() {
      if (selectedTopic && kafkaId) {
        setIsFetching(true);
        try {
          const response = await gettopicMetrics(kafkaId, selectedTopic);
          const ranges = response.payload?.data?.attributes?.metrics?.ranges;

          if (ranges) {
            setTopicSpecificMetrics({
              incoming: timeSeriesMetrics(ranges, "incoming_byte_rate"),
              outgoing: timeSeriesMetrics(ranges, "outgoing_byte_rate"),
            });
          }
        } catch (error) {
          console.error("Failed to fetch topic metrics", error);
        } finally {
          setIsFetching(false);
        }
      } else {
        setTopicSpecificMetrics(null);
      }
    }

    fetchSpecificMetrics();
  }, [selectedTopic, kafkaId]);

  const displayIncoming = topicSpecificMetrics?.incoming ?? incoming;

  const displayOutgoing = topicSpecificMetrics?.outgoing ?? outgoing;

  const hasMetrics =
    !isVirtualKafkaCluster &&
    Object.keys(displayIncoming ?? {}).length > 0 &&
    Object.keys(displayOutgoing ?? {}).length > 0;

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <Title headingLevel={"h2"} size={"lg"}>
            {t("topicMetricsCard.topic_metric")}
          </Title>
        </CardTitle>
      </CardHeader>
      <CardBody>
        <Flex direction={{ default: "column" }} gap={{ default: "gapLg" }}>
          <b>
            {t("topicMetricsCard.topics_bytes_incoming_and_outgoing")}{" "}
            <Tooltip
              content={t(
                "topicMetricsCard.topics_bytes_incoming_and_outgoing_tooltip",
              )}
            >
              <HelpIcon />
            </Tooltip>
          </b>
          {isLoading ? (
            <ChartSkeletonLoader />
          ) : (
            <>
              {hasMetrics && (
                <FilterByTopic
                  selectedTopic={selectedTopic}
                  topicList={topicList}
                  onSetSelectedTopic={setSelectedTopic}
                  disableToolbar={topicList.length === 0 || isFetching}
                />
              )}

              <ChartIncomingOutgoing
                incoming={displayIncoming || {}}
                outgoing={displayOutgoing || {}}
                isVirtualKafkaCluster={isVirtualKafkaCluster}
                selectedTopicName={selectedTopicName}
              />
            </>
          )}
        </Flex>
      </CardBody>
    </Card>
  );
}
