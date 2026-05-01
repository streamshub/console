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
  EmptyStateFooter,
  EmptyStateActions,
  Title,
  List,
  ListItem,
  Content,
  Tooltip,
} from '@patternfly/react-core';
import { CubesIcon, HelpIcon, ExternalLinkAltIcon } from '@patternfly/react-icons';
import { ViewedTopic } from '@/api/hooks/useViewedTopics';
import { useShowLearning } from '@/hooks/useShowLearning';

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
  const showLearning = useShowLearning();

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
              {showLearning && (
                <EmptyStateFooter>
                  <EmptyStateActions>
                    <a
                      href={t('learning.links.topicOperatorUse')}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{ fontSize: 'var(--pf-v6-global--FontSize--sm)' }}
                    >
                      {t('learning.labels.topicOperatorUse')} <ExternalLinkAltIcon />
                    </a>
                  </EmptyStateActions>
                </EmptyStateFooter>
              )}
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