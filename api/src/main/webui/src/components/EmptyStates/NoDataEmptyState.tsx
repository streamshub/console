/**
 * No Data Empty State Component
 * Displays when there is no data available (not filtered)
 */

import { useTranslation } from 'react-i18next';
import {
  EmptyState,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateActions,
  Title,
} from '@patternfly/react-core';
import { CubesIcon, ExternalLinkAltIcon } from '@patternfly/react-icons';

interface NoDataEmptyStateProps {
  entityName?: string;
  title?: string;
  message?: string;
  icon?: React.ComponentType;
  learningLink?: {
    href: string;
    label: string;
  };
}

export function NoDataEmptyState({
  entityName,
  title,
  message,
  icon: IconComponent = CubesIcon,
  learningLink,
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
      {learningLink && (
        <EmptyStateFooter>
          <EmptyStateActions>
            <a
              href={learningLink.href}
              target="_blank"
              rel="noopener noreferrer"
            >
              {learningLink.label} <ExternalLinkAltIcon />
            </a>
          </EmptyStateActions>
        </EmptyStateFooter>
      )}
    </EmptyState>
  );
}