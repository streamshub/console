import {
  Flex,
  FlexItem,
  Grid,
  GridItem,
  PageSection,
} from "@/libs/patternfly/react-core";
import { ReactNode, Suspense } from "react";
import { ClusterCard } from "./ClusterCard";
import { ClusterChartsCard } from "./ClusterChartsCard";
import { TopicChartsCard } from "./TopicChartsCard";
import { TopicsPartitionsCard } from "./TopicsPartitionsCard";

export function PageLayout({
  clusterOverview,
  clusterCharts,
  topicsPartitions,
  topicCharts,
}: {
  clusterOverview: ReactNode;
  topicsPartitions: ReactNode;
  clusterCharts: ReactNode;
  topicCharts: ReactNode;
}) {
  return (
    <PageSection isFilled>
      <Grid hasGutter={true}>
        <GridItem md={7}>
          <Flex direction={{ default: "column" }}>
            <FlexItem>
              <Suspense fallback={<ClusterCard isLoading={true} />}>
                {clusterOverview}
              </Suspense>
            </FlexItem>
            <FlexItem>
              <Suspense fallback={<ClusterChartsCard isLoading={true} />}>
                {clusterCharts}
              </Suspense>
            </FlexItem>
          </Flex>
        </GridItem>
        <GridItem md={5}>
          <Flex direction={{ default: "column" }}>
            <FlexItem>
              <Suspense fallback={<TopicsPartitionsCard isLoading={true} />}>
                {topicsPartitions}
              </Suspense>
            </FlexItem>
            <FlexItem>
              <Suspense fallback={<TopicChartsCard isLoading={true} />}>
                {topicCharts}
              </Suspense>
            </FlexItem>
          </Flex>
        </GridItem>
      </Grid>
    </PageSection>
  );
}
