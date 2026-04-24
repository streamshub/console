/**
 * No Results Empty State Component
 * Displays when filters/search return no results
 */

import { useTranslation } from 'react-i18next';
import {
  EmptyState,
  EmptyStateBody,
  Title,
} from '@patternfly/react-core';
import { SearchIcon } from '@patternfly/react-icons';

interface NoResultsEmptyStateProps {
  title?: string;
  message?: string;
}

export function NoResultsEmptyState({ title, message }: NoResultsEmptyStateProps = {}) {
  const { t } = useTranslation();

  return (
    <EmptyState>
      <SearchIcon />
      <Title headingLevel="h2" size="lg">
        {title || t('common.noResultsFound')}
      </Title>
      <EmptyStateBody>
        {message || t('common.noResultsFoundDescription')}
      </EmptyStateBody>
    </EmptyState>
  );
}