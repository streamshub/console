"use client";
import { MetricRange } from "@/api/kafka/schema";
import { ChartIncomingOutgoing } from "@/app/[locale]/kafka/[kafkaId]/overview/ChartIncomingOutgoing";
import { ChartSkeletonLoader } from "@/app/[locale]/kafka/[kafkaId]/overview/ChartSkeletonLoader";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Flex,
} from "@/libs/patternfly/react-core";
import { Title, Tooltip } from "@patternfly/react-core";
import { HelpIcon } from "@patternfly/react-icons";

type TopicChartsCardProps = {
  incoming: MetricRange;
  outgoing: MetricRange;
};

export function TopicChartsCard({
  isLoading,
  incoming,
  outgoing,
}:
  | ({ isLoading: false } & TopicChartsCardProps)
  | ({
      isLoading: true;
    } & Partial<{ [key in keyof TopicChartsCardProps]?: undefined }>)) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <Title headingLevel={"h2"} size={"lg"}>
            Topic metrics
          </Title>
        </CardTitle>
      </CardHeader>
      <CardBody>
        <Flex direction={{ default: "column" }} gap={{ default: "gapLg" }}>
          <b>
            Topics bytes incoming and outgoing{" "}
            <Tooltip
              content={
                "Bytes incoming and outgoing are the total bytes for all topics or total bytes for a selected topic in the Kafka cluster. This metric enables you to assess data transfer in and out of your Kafka cluster. To modify incoming and outgoing bytes, you can adjust topic message size or other topic properties as needed."
              }
            >
              <HelpIcon />
            </Tooltip>
          </b>
          {isLoading ? (
            <ChartSkeletonLoader />
          ) : (
            <ChartIncomingOutgoing incoming={incoming} outgoing={outgoing} />
          )}
        </Flex>
      </CardBody>
    </Card>
  );
}
