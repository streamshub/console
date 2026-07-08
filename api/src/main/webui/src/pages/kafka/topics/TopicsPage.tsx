/**
 * Topics Page - List all topics in a Kafka cluster
 */

import { useCallback, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Title,
  Spinner,
  Tooltip,
  Label,
  Split,
  SplitItem,
} from '@patternfly/react-core';
import {
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ExclamationCircleIcon,
} from '@patternfly/react-icons';
import { useTopics } from '@/api/hooks/useTopics';
import { ResourceListParams } from '@/api/hooks/useResourceList';
import { TopicStatusSummary } from '@/api/types';
import { TopicsDataView } from '@/components/kafka/topics/TopicsDataView';

const TOPIC_LISTING_FIELDS = 'name,status,numPartitions,totalLeaderLogBytes,groups';

export function TopicsPage() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  const [dataParams, setDataParams] = useState<ResourceListParams>({
    fields: TOPIC_LISTING_FIELDS,
  });

  const topicResult = useTopics(kafkaId, dataParams);
  const { isLoading } = topicResult;
  const totalItems = topicResult.data?.meta?.page?.total || 0;
  const statusSummary = (topicResult.data?.meta?.summary as { statuses?: TopicStatusSummary } | undefined)?.statuses;

  const handleDataViewChange = useCallback((params: ResourceListParams) => {
    setDataParams({
      ...params,
      fields: TOPIC_LISTING_FIELDS,
    });
  }, []);

  return (
    <>
      <PageSection>
        <Title headingLevel="h1" size="2xl">
          <Split hasGutter style={{ alignItems: 'center', display: 'flex' }}>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>{t('topics.title')}</SplitItem>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>
              <Label icon={isLoading ? <Spinner size="sm" /> : undefined}>
                {totalItems}&nbsp;total
              </Label>
            </SplitItem>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>
              <Tooltip content={t('topics.statusLabels.fullyReplicatedTooltip', 'Number of topics in sync')}>
                <Label
                  icon={isLoading ? <Spinner size="sm" /> : <CheckCircleIcon />}
                  color="green"
                >
                  {statusSummary?.FullyReplicated ?? 0}
                </Label>
              </Tooltip>
            </SplitItem>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>
              <Tooltip content={t('topics.statusLabels.underReplicatedTooltip', 'Number of topics under replicated')}>
                <Label
                  icon={isLoading ? <Spinner size="sm" /> : <ExclamationTriangleIcon />}
                  color="orange"
                >
                  {(statusSummary?.UnderReplicated ?? 0) + (statusSummary?.PartiallyOffline ?? 0) + (statusSummary?.Unknown ?? 0)}
                </Label>
              </Tooltip>
            </SplitItem>
            <SplitItem style={{ display: 'flex', alignItems: 'center' }}>
              <Tooltip content={t('topics.statusLabels.offlineTooltip', 'Number of topics not available')}>
                <Label
                  icon={isLoading ? <Spinner size="sm" /> : <ExclamationCircleIcon />}
                  color="red"
                >
                  {statusSummary?.Offline ?? 0}
                </Label>
              </Tooltip>
            </SplitItem>
          </Split>
        </Title>
      </PageSection>
      <PageSection>
        <TopicsDataView
          topicResult={topicResult}
          onDataViewChange={handleDataViewChange}
        />
      </PageSection>
    </>
  );
}
