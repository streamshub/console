/**
 * Node Configuration Tab - Shows configuration settings for a Kafka node/broker
 */

import { useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  EmptyState,
  EmptyStateBody,
  Title,
  Spinner,
} from '@patternfly/react-core';
import { useNodeConfig } from '@/api/hooks/useNodes';
import { ConfigurationTable } from '@/components/common/ConfigurationTable';

export function NodeConfigurationTab() {
  const { t } = useTranslation();
  const { kafkaId, nodeId } = useParams<{ kafkaId: string; nodeId: string }>();

  const { data, isLoading, error } = useNodeConfig(kafkaId, nodeId, {
    includeDocumentation: true,
  });

  if (isLoading) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Spinner size="xl" />
          <Title headingLevel="h2" size="lg">
            {t('common.loading')}
          </Title>
        </EmptyState>
      </PageSection>
    );
  }

  if (error) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Title headingLevel="h2" size="lg">
            {t('common.error')}
          </Title>
          <EmptyStateBody>{error.message}</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  return (
    <ConfigurationTable
      configs={data?.data?.attributes}
      ariaLabel="Node configuration table"
      ouiaId="node-configuration-listing"
      isFilled
      showDocumentation
    />
  );
}
