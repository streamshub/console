import { DateTime } from "@/components/Format/DateTime";
import {
  Flex,
  FlexItem,
  Grid,
  GridItem,
  PageSection,
} from "@/libs/patternfly/react-core";
import { Link } from "@/i18n/routing";
import { ReactNode, Suspense } from "react";
import { ClusterCard } from "./ClusterCard";
import { ClusterChartsCard } from "./ClusterChartsCard";
import { RecentTopicsCard } from "./RecentTopicsCard";
import { TopicChartsCard } from "./TopicChartsCard";
import { TopicsPartitionsCard } from "./TopicsPartitionsCard";

export function PageLayout({
  clusterOverview,
  clusterCharts,
  topicsPartitions,
  topicCharts,
  recentTopics,
}: {
  clusterOverview: ReactNode;
  topicsPartitions: ReactNode;
  clusterCharts: ReactNode;
  topicCharts: ReactNode;
  recentTopics: ReactNode;
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
              <Suspense
                fallback={
                  <RecentTopicsCard viewedTopics={[]} isLoading={true} />
                }
              >
                {recentTopics}
              </Suspense>
            </FlexItem>
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
