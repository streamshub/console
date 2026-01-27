"use client";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Flex,
  Title,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { ChartIncomingOutgoing } from "./components/ChartIncomingOutgoing";
import { ChartSkeletonLoader } from "./components/ChartSkeletonLoader";
import { useTranslations } from "next-intl";
import { FilterByTopic } from "./components/FilterByTopic";
import { useState } from "react";
import { FilterByTime } from "./components/FilterByTime";
import { useTopicMetrics } from "./components/useTopicMetrics";
import { DurationOptions } from "./components/type";

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
  const [duration, setDuration] = useState<DurationOptions>(
    DurationOptions.Last5minutes,
  );
  const { data, isLoading: isFetching } = useTopicMetrics(
    kafkaId,
    selectedTopic,
    duration,
  );

  const displayIncoming = data?.incoming_byte_rate ?? incoming;
  const displayOutgoing = data?.outgoing_byte_rate ?? outgoing;

  const selectedTopicName = topicList?.find(
    (t) => t.id === selectedTopic,
  )?.name;

  const hasBaselineMetrics =
    !isVirtualKafkaCluster &&
    Object.keys(incoming ?? {}).length > 0 &&
    Object.keys(outgoing ?? {}).length > 0;

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
              {hasBaselineMetrics && (
                <Toolbar>
                  <ToolbarContent>
                    <ToolbarGroup variant="filter-group">
                      <FilterByTopic
                        selectedTopic={selectedTopic}
                        topicList={topicList}
                        onSetSelectedTopic={setSelectedTopic}
                        disableToolbar={isFetching}
                      />
                      <FilterByTime
                        duration={duration}
                        onDurationChange={setDuration}
                        disableToolbar={isFetching}
                        ariaLabel={"Select time range"}
                      />
                    </ToolbarGroup>
                  </ToolbarContent>
                </Toolbar>
              )}

              <ChartIncomingOutgoing
                incoming={displayIncoming || {}}
                outgoing={displayOutgoing || {}}
                isVirtualKafkaCluster={isVirtualKafkaCluster}
                selectedTopicName={selectedTopicName}
                duration={duration}
              />
            </>
          )}
        </Flex>
      </CardBody>
    </Card>
  );
}
