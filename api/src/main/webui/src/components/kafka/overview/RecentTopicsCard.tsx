/**
 * Recent Topics Card Component
 *
 * Displays the last 5 viewed topics for quick access.
 * Uses localStorage to track topic views.
 */

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Card,
  CardBody,
  CardExpandableContent,
  CardHeader,
  CardTitle,
  Divider,
  EmptyState,
  EmptyStateBody,
  Title,
  List,
  ListItem,
  Content,
  Tooltip,
} from '@patternfly/react-core';
import { CubesIcon, HelpIcon } from '@patternfly/react-icons';
import { ViewedTopic } from '@/api/hooks/useViewedTopics';

export interface RecentTopicsCardProps {
  viewedTopics: ViewedTopic[];
  isLoading?: boolean;
}

export function RecentTopicsCard({
  viewedTopics,
  isLoading = false,
}: RecentTopicsCardProps) {
  const { t } = useTranslation();
  const [isExpanded, setIsExpanded] = useState(true);

  return (
    <Card component="div" isFullHeight isExpanded={isExpanded} isCompact>
      <CardHeader onExpand={() => setIsExpanded(!isExpanded)}>
        <Content>
          <CardTitle>
            <b>
              {t('RecentTopicsCard.recent_topics')}{' '}
              <Tooltip
                content={t('RecentTopicsCard.recent_topics_tooltip')}
              >
                <HelpIcon />
              </Tooltip>
            </b>
          </CardTitle>
          <Content component="small">
            {t('RecentTopicsCard.recent_topics_subtitle')}
          </Content>
        </Content>
      </CardHeader>
      <CardExpandableContent>
        <CardBody>
          {isLoading ? (
            <EmptyState>
              <Title headingLevel="h4" size="lg">
                {t('common.loading')}
              </Title>
            </EmptyState>
          ) : viewedTopics.length === 0 ? (
            <EmptyState variant="xs">
              <CubesIcon />
              <Title headingLevel="h4" size="lg">
                {t('RecentTopicsCard.no_recent_topics')}
              </Title>
              <EmptyStateBody>
                {t('RecentTopicsCard.no_recent_topics_description')}
              </EmptyStateBody>
            </EmptyState>
          ) : (
            <>
              <Divider />
              <List isPlain style={{ paddingTop: '1rem' }}>
                {viewedTopics.map((topic) => (
                  <ListItem key={topic.topicId}>
                    <Link
                      to={`../topics/${topic.topicId}`}
                      style={{ textDecoration: 'none' }}
                    >
                      {topic.topicName}
                    </Link>
                  </ListItem>
                ))}
              </List>
            </>
          )}
        </CardBody>
      </CardExpandableContent>
    </Card>
  );
}