/**
 * Home Page - Kafka Cluster Selection
 */

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Title,
  EmptyState,
  EmptyStateBody,
  Skeleton,
} from '@patternfly/react-core';
import { useMetadata } from '../api/hooks/useMetadata';
import { AppLayout } from '@/components/app/AppLayout';
import { ClustersDataView } from '@/components/home/ClustersDataView';
import { ResourceListParams } from '@/api/hooks/useResourceList';
import { useKafkaClusters } from '@/api/hooks/useKafkaClusters';

export function HomePage() {
  const { t } = useTranslation();
  const [dataParams, setDataParams] = useState<ResourceListParams>({});
  const { data: clusterResponse, isLoading, error } = useKafkaClusters(dataParams);
  const { data: metadata } = useMetadata();

  const totalCount = clusterResponse?.meta?.page?.total;
  const platform = metadata?.data?.attributes?.platform;

  if (error) {
    return (
      <AppLayout>
        <PageSection>
          <EmptyState>
            <Title headingLevel="h1" size="lg">
              {t('common.error')}
            </Title>
            <EmptyStateBody>{error.message}</EmptyStateBody>
          </EmptyState>
        </PageSection>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      <PageSection variant="default">
        {platform && (
          <p>
            <strong>{t('common.platform', 'Platform')}:</strong> {platform}
          </p>
        )}
        <p>
          { totalCount 
            ? totalCount + ' ' + t('kafka.connectedClusters') 
            : <Skeleton width='5px' style={{display: 'inline'}}/>
          }
        </p>
      </PageSection>
      <PageSection>
        <ClustersDataView
          clusterResponse={clusterResponse}
          isLoading={isLoading}
          onDataViewChange={setDataParams}
        />
      </PageSection>
    </AppLayout>
  );
}