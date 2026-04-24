/**
 * No Data Empty State Component
 * Displays when there is no data available (not filtered)
 */

import { useTranslation } from 'react-i18next';
import {
  EmptyState,
  EmptyStateBody,
  Title,
} from '@patternfly/react-core';
import { CubesIcon } from '@patternfly/react-icons';

interface NoDataEmptyStateProps {
  entityName?: string;
  title?: string;
  message?: string;
  icon?: React.ComponentType;
}

export function NoDataEmptyState({ 
  entityName, 
  title, 
  message,
  icon: IconComponent = CubesIcon 
}: NoDataEmptyStateProps = {}) {
  const { t } = useTranslation();

  const defaultTitle = entityName 
    ? t('common.noEntityAvailable', { entity: entityName })
    : t('common.noData');

  const defaultMessage = entityName
    ? t('common.noEntityDescription', { entity: entityName })
    : t('common.noDataDescription');

  return (
    <EmptyState>
      <IconComponent />
      <Title headingLevel="h2" size="lg">
        {title || defaultTitle}
      </Title>
      <EmptyStateBody>
        {message || defaultMessage}
      </EmptyStateBody>
    </EmptyState>
  );
}