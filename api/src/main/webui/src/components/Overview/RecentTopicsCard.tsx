/**
 * Recent Topics Card Component
 *
 * Displays the last 5 viewed topics for quick access.
 * Uses localStorage to track topic views.
 */

import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Card,
  CardBody,
  CardTitle,
  EmptyState,
  EmptyStateBody,
  Title,
  List,
  ListItem,
} from '@patternfly/react-core';
import { CubesIcon } from '@patternfly/react-icons';
import { ViewedTopic } from '../../api/hooks/useViewedTopics';

export interface RecentTopicsCardProps {
  viewedTopics: ViewedTopic[];
  isLoading?: boolean;
}

export function RecentTopicsCard({
  viewedTopics,
  isLoading = false,
}: RecentTopicsCardProps) {
  const { t } = useTranslation();

  return (
    <Card component="div" isFullHeight>
      <CardTitle>{t('RecentTopicsCard.recent_topics')}</CardTitle>
      <CardBody>
        {isLoading ? (
          <EmptyState>
            <Title headingLevel="h4" size="lg">
              {t('common.loading')}
            </Title>
          </EmptyState>
        ) : viewedTopics.length === 0 ? (
          <EmptyState>
            <CubesIcon />
            <Title headingLevel="h4" size="lg">
              {t('RecentTopicsCard.no_recent_topics')}
            </Title>
            <EmptyStateBody>
              {t('RecentTopicsCard.no_recent_topics_description')}
            </EmptyStateBody>
          </EmptyState>
        ) : (
          <List isPlain>
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
        )}
      </CardBody>
    </Card>
  );
}