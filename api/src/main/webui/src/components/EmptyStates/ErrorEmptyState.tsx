/**
 * Error Empty State Component
 * Displays an error message with icon
 */

import { useTranslation } from 'react-i18next';
import {
  EmptyState,
  EmptyStateBody,
  Title,
} from '@patternfly/react-core';
import { ExclamationTriangleIcon } from '@patternfly/react-icons';

interface ErrorEmptyStateProps {
  error?: Error | unknown;
  title?: string;
  message?: string;
}

export function ErrorEmptyState({ error, title, message }: ErrorEmptyStateProps = {}) {
  const { t } = useTranslation();

  const errorMessage = message || (error instanceof Error ? error.message : t('common.errorLoading'));

  return (
    <EmptyState>
      <ExclamationTriangleIcon />
      <Title headingLevel="h1" size="lg">
        {title || t('common.error')}
      </Title>
      <EmptyStateBody>{errorMessage}</EmptyStateBody>
    </EmptyState>
  );
}