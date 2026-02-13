/**
 * Topics Filter Toolbar Component
 * Provides dropdown selector and dynamic input based on filter type
 */

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Select,
  SelectOption,
  SelectList,
  MenuToggle,
  MenuToggleElement,
  TextInput,
  InputGroup,
  MenuToggleStatus,
} from '@patternfly/react-core';
import {
  FilterIcon,
} from '@patternfly/react-icons';
import { TopicsFilterType, TopicStatus } from './topicsFilterTypes';
import { StatusLabel } from '@/components/StatusLabel';
import { TOPIC_STATUS_CONFIG } from '@/components/StatusLabel/configs';

interface TopicsFilterToolbarProps {
  selectedFilterType: TopicsFilterType;
  onFilterTypeChange: (type: TopicsFilterType) => void;
  filterValue: string;
  onFilterValueChange: (value: string) => void;
  onFilterSubmit: () => void;
  selectedStatuses: TopicStatus[];
  onStatusToggle: (status: TopicStatus) => void;
}

const TOPIC_STATUSES: TopicStatus[] = [
  'FullyReplicated',
  'UnderReplicated',
  'PartiallyOffline',
  'Offline',
  'Unknown',
];

export function TopicsFilterToolbar({
  selectedFilterType,
  onFilterTypeChange,
  filterValue,
  onFilterValueChange,
  onFilterSubmit,
  selectedStatuses,
  onStatusToggle,
}: TopicsFilterToolbarProps) {
  const { t } = useTranslation();
  const [isFilterTypeOpen, setIsFilterTypeOpen] = useState(false);
  const [isStatusOpen, setIsStatusOpen] = useState(false);

  const filterTypeOptions: { value: TopicsFilterType; label: string }[] = [
    { value: 'name', label: t('topics.filter.name') },
    { value: 'id', label: t('topics.filter.topicId') },
    { value: 'status', label: t('topics.filter.status') },
  ];

  const handleFilterTypeSelect = (
    _event: React.MouseEvent<Element, MouseEvent> | undefined,
    value: string | number | undefined
  ) => {
    onFilterTypeChange(value as TopicsFilterType);
    setIsFilterTypeOpen(false);
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      onFilterSubmit();
    }
  };

  const getFilterTypeLabel = () => {
    const option = filterTypeOptions.find((opt) => opt.value === selectedFilterType);
    return option?.label || '';
  };


  const renderFilterInput = () => {
    if (selectedFilterType === 'status') {
      return (
        <Select
          id="status-filter-select"
          isOpen={isStatusOpen}
          selected={selectedStatuses}
          onSelect={(_, value) => onStatusToggle(value as TopicStatus)}
          onOpenChange={(isOpen) => setIsStatusOpen(isOpen)}
          toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
            <MenuToggle
              ref={toggleRef}
              onClick={() => setIsStatusOpen(!isStatusOpen)}
              isExpanded={isStatusOpen}
              status={selectedStatuses.length > 0 ? MenuToggleStatus.success : undefined}
              style={{ width: '300px' }}
            >
              {selectedStatuses.length > 0
                ? t('topics.filter.statusSelected', { count: selectedStatuses.length })
                : t('topics.filter.selectStatus')}
            </MenuToggle>
          )}
        >
          <SelectList>
            {TOPIC_STATUSES.map((status) => (
              <SelectOption
                key={status}
                value={status}
                hasCheckbox
                isSelected={selectedStatuses.includes(status)}
              >
                <StatusLabel status={status} config={TOPIC_STATUS_CONFIG} />
              </SelectOption>
            ))}
          </SelectList>
        </Select>
      );
    }

    return (
      <TextInput
        type="text"
        id={`${selectedFilterType}-filter-input`}
        placeholder={
          selectedFilterType === 'name'
            ? t('topics.filter.namePlaceholder')
            : t('topics.filter.idPlaceholder')
        }
        value={filterValue}
        onChange={(_event, value) => onFilterValueChange(value)}
        onKeyDown={handleKeyDown}
        aria-label={
          selectedFilterType === 'name'
            ? t('topics.filter.nameLabel')
            : t('topics.filter.idLabel')
        }
        style={{ minWidth: '300px' }}
      />
    );
  };

  return (
    <InputGroup>
      <Select
        id="filter-type-select"
        isOpen={isFilterTypeOpen}
        selected={selectedFilterType}
        onSelect={handleFilterTypeSelect}
        onOpenChange={(isOpen) => setIsFilterTypeOpen(isOpen)}
        toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
          <MenuToggle
            ref={toggleRef}
            onClick={() => setIsFilterTypeOpen(!isFilterTypeOpen)}
            isExpanded={isFilterTypeOpen}
            icon={<FilterIcon />}
            style={{ width: 'auto', minWidth: '140px' } as React.CSSProperties}
          >
            {getFilterTypeLabel()}
          </MenuToggle>
        )}
      >
        <SelectList>
          {filterTypeOptions.map((option) => (
            <SelectOption key={option.value} value={option.value}>
              {option.label}
            </SelectOption>
          ))}
        </SelectList>
      </Select>
      {renderFilterInput()}
    </InputGroup>
  );
}