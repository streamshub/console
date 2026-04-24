/**
 * Topics Filter Chips Component
 * Displays active filters as removable chips with "Clear all filters" link
 */

import { useTranslation } from 'react-i18next';
import {
  ToolbarGroup,
  ToolbarItem,
  Label,
  LabelGroup,
  Button,
  Icon,
} from '@patternfly/react-core';
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  ExclamationCircleIcon,
} from '@patternfly/react-icons';
import { TopicsFilterState, TopicStatus } from './topicsFilterTypes';

interface TopicsFilterChipsProps {
  filters: TopicsFilterState;
  onRemoveFilter: (type: 'name' | 'id' | 'status', value?: string) => void;
  onClearAllFilters: () => void;
}

export function TopicsFilterChips({
  filters,
  onRemoveFilter,
  onClearAllFilters,
}: TopicsFilterChipsProps) {
  const { t } = useTranslation();

  const hasActiveFilters =
    !!filters.name || !!filters.id || (filters.status && filters.status.length > 0);

  if (!hasActiveFilters) {
    return null;
  }

  const getStatusLabel = (status: TopicStatus) => {
    switch (status) {
      case 'FullyReplicated':
        return (
          <>
            <Icon status="success">
              <CheckCircleIcon />
            </Icon>
            &nbsp; {t('topics.status.FullyReplicated')}
          </>
        );
      case 'UnderReplicated':
        return (
          <>
            <Icon status="warning">
              <ExclamationTriangleIcon />
            </Icon>
            &nbsp;{t('topics.status.UnderReplicated')}
          </>
        );
      case 'PartiallyOffline':
        return (
          <>
            <Icon status="warning">
              <ExclamationTriangleIcon />
            </Icon>
            &nbsp;{t('topics.status.PartiallyOffline')}
          </>
        );
      case 'Unknown':
        return (
          <>
            <Icon status="warning">
              <ExclamationTriangleIcon />
            </Icon>
            &nbsp;{t('topics.status.Unknown')}
          </>
        );
      case 'Offline':
        return (
          <>
            <Icon status="danger">
              <ExclamationCircleIcon />
            </Icon>
            &nbsp;{t('topics.status.Offline')}
          </>
        );
      default:
        return status;
    }
  };

  return (
    <ToolbarGroup>
      {filters.name && (
        <ToolbarItem>
          <LabelGroup categoryName={t('topics.filter.name')}>
            <Label
              color="blue"
              onClose={() => onRemoveFilter('name')}
              isCompact
            >
              {filters.name}
            </Label>
          </LabelGroup>
        </ToolbarItem>
      )}

      {filters.id && (
        <ToolbarItem>
          <LabelGroup categoryName={t('topics.filter.topicId')}>
            <Label
              color="blue"
              onClose={() => onRemoveFilter('id')}
              isCompact
            >
              {filters.id}
            </Label>
          </LabelGroup>
        </ToolbarItem>
      )}

      {filters.status && filters.status.length > 0 && (
        <ToolbarItem>
          <LabelGroup
            categoryName={t('topics.filter.status')}
            numLabels={3}
          >
            {filters.status.map((status) => (
              <Label
                key={status}
                color="blue"
                onClose={() => onRemoveFilter('status', status)}
                isCompact
              >
                {getStatusLabel(status)}
              </Label>
            ))}
          </LabelGroup>
        </ToolbarItem>
      )}

      <ToolbarItem>
        <Button variant="link" onClick={onClearAllFilters}>
          {t('topics.filter.clearAllFilters')}
        </Button>
      </ToolbarItem>
    </ToolbarGroup>
  );
}