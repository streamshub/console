"use client";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Flex,
  FlexItem,
  Switch,
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
import { useEffect, useState } from "react";
import { FilterByTime } from "./components/FilterByTime";
import { useTopicMetrics } from "./components/useTopicMetrics";
import { DurationOptions } from "./components/type";
import { usePathname } from "next/navigation";

type Visibility = "internal" | "external";

export type TopicOption = {
  id: string;
  name: string;
  managed?: boolean;
  visibility?: Visibility;
};

type TopicChartsCardProps = {
  incoming: TimeSeriesMetrics;
  outgoing: TimeSeriesMetrics;
  topicList: TopicOption[];
  kafkaId: string | undefined;
  includeHidden?: boolean;
};

export function TopicChartsCard({
  isLoading,
  incoming,
  outgoing,
  includeHidden: initialIncludeHidden,
  isVirtualKafkaCluster,
  topicList = [],
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
  const pathname = usePathname();
  const [hideInternal, setHideInternal] = useState(!initialIncludeHidden);

  const [selectedTopic, setSelectedTopic] = useState<string | undefined>();

  const filteredTopicList = hideInternal
    ? topicList.filter((t) => t.visibility !== "internal")
    : topicList;

  useEffect(() => {
    if (!selectedTopic) return;

    const existsInFiltered = filteredTopicList.some(
      (t) => t.id === selectedTopic,
    );

    if (!existsInFiltered) {
      setSelectedTopic(undefined);
    }
  }, [filteredTopicList, selectedTopic]);

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

  const onInternalTopicsChange = (checked: boolean) => {
    setHideInternal(checked);

    const params = new URLSearchParams(window.location.search);
    if (checked) {
      params.delete("includeHidden");
    } else {
      params.set("includeHidden", "true");
    }
    window.history.replaceState(null, "", `${pathname}?${params.toString()}`);
  };

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
                      <Flex>
                        <Flex>
                          <FlexItem>
                            <Switch
                              key={"ht"}
                              label={
                                <>
                                  {t("topics.hide_internal_topics")}&nbsp;
                                  <Tooltip
                                    content={t(
                                      "topics.hide_internal_topics_tooltip",
                                    )}
                                  >
                                    <HelpIcon />
                                  </Tooltip>
                                </>
                              }
                              isChecked={hideInternal}
                              onChange={(_, checked) =>
                                onInternalTopicsChange(checked)
                              }
                              className={"pf-v6-u-py-xs"}
                            />
                          </FlexItem>
                        </Flex>
                        <Flex>
                          <FlexItem>
                            <FilterByTopic
                              selectedTopic={selectedTopic}
                              topicList={filteredTopicList}
                              onSetSelectedTopic={setSelectedTopic}
                              disableToolbar={isFetching}
                            />
                          </FlexItem>
                          <FlexItem>
                            <FilterByTime
                              duration={duration}
                              onDurationChange={setDuration}
                              disableToolbar={isFetching}
                              ariaLabel={"Select time range"}
                            />
                          </FlexItem>
                        </Flex>
                      </Flex>
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
