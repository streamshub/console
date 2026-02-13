/**
 * Overview Layout Component
 * 
 * Provides the 2-column grid layout for the Kafka cluster overview page.
 * Left column (7/12): ClusterCard, ClusterChartsCard
 * Right column (5/12): RecentTopicsCard, TopicsPartitionsCard, TopicChartsCard
 */

import { ReactNode } from 'react';
import {
  Grid,
  GridItem,
  Flex,
  FlexItem,
  PageSection,
} from '@patternfly/react-core';

export interface OverviewLayoutProps {
  clusterCard: ReactNode;
  clusterChartsCard: ReactNode;
  recentTopicsCard: ReactNode;
  topicsPartitionsCard: ReactNode;
  topicChartsCard: ReactNode;
}

export function OverviewLayout({
  clusterCard,
  clusterChartsCard,
  recentTopicsCard,
  topicsPartitionsCard,
  topicChartsCard,
}: OverviewLayoutProps) {
  return (
    <PageSection isFilled>
      <Grid hasGutter>
        {/* Left Column - 7/12 width */}
        <GridItem md={7}>
          <Flex direction={{ default: 'column' }}>
            <FlexItem>{clusterCard}</FlexItem>
            <FlexItem>{clusterChartsCard}</FlexItem>
          </Flex>
        </GridItem>

        {/* Right Column - 5/12 width */}
        <GridItem md={5}>
          <Flex direction={{ default: 'column' }}>
            <FlexItem>{recentTopicsCard}</FlexItem>
            <FlexItem>{topicsPartitionsCard}</FlexItem>
            <FlexItem>{topicChartsCard}</FlexItem>
          </Flex>
        </GridItem>
      </Grid>
    </PageSection>
  );
}