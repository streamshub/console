/**
 * Loading Empty State Component
 * Displays a loading spinner with message
 */

import { useTranslation } from 'react-i18next';
import {
  EmptyState,
  EmptyStateBody,
  Spinner,
  Title,
} from '@patternfly/react-core';

interface LoadingEmptyStateProps {
  message?: string;
}

export function LoadingEmptyState({ message }: LoadingEmptyStateProps = {}) {
  const { t } = useTranslation();

  return (
    <EmptyState>
      <Spinner size="xl" />
      <Title headingLevel="h1" size="lg">
        {t('common.loading')}
      </Title>
      {message && <EmptyStateBody>{message}</EmptyStateBody>}
    </EmptyState>
  );
}