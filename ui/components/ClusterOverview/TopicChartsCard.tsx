"use client";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Flex,
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
import { useEffect, useOptimistic, useState, useTransition } from "react";
import { FilterByTime } from "./components/FilterByTime";
import { useTopicMetrics } from "./components/useTopicMetrics";
import { DurationOptions } from "./components/type";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

type TopicOption = { id: string; name: string; managed?: boolean };

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

  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const [isPending, startTransition] = useTransition();
  const [optimisticIncludeHidden, setOptimisticIncludeHidden] = useOptimistic(
    initialIncludeHidden,
    (_, newValue: boolean) => newValue,
  );

  const includeHidden = optimisticIncludeHidden;

  const [selectedTopic, setSelectedTopic] = useState<string | undefined>();

  useEffect(() => {
    if (selectedTopic && topicList && topicList.length > 0) {
      const exists = topicList.some((t) => t.id === selectedTopic);
      if (!exists) {
        setSelectedTopic(undefined);
      }
    }
  }, [topicList, selectedTopic]);

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

  const onInternalTopicsChange = (hideChecked: boolean) => {
    const shouldInclude = !hideChecked;

    startTransition(() => {
      setOptimisticIncludeHidden(shouldInclude);
      const params = new URLSearchParams(searchParams.toString());
      if (shouldInclude) {
        params.set("includeHidden", "true");
      } else {
        params.delete("includeHidden");
      }
      router.push(`${pathname}?${params.toString()}`, { scroll: false });
    });
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
                      <Switch
                        key={"ht"}
                        label={
                          <>
                            {t("topics.hide_internal_topics")}&nbsp;
                            <Tooltip
                              content={t("topics.hide_internal_topics_tooltip")}
                            >
                              <HelpIcon />
                            </Tooltip>
                          </>
                        }
                        isChecked={!optimisticIncludeHidden}
                        onChange={(_, checked) =>
                          onInternalTopicsChange(checked)
                        }
                        className={"pf-v6-u-py-xs"}
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
