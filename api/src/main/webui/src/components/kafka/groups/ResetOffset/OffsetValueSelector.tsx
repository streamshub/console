/**
 * Offset Value Selector Component
 * Allows selection of offset type and value input
 */

import { useTranslation } from 'react-i18next';
import {
  FormGroup,
  TextInput,
  Menu,
  MenuList,
  MenuContent,
  SelectOption,
  MenuToggle,
  MenuContainer,
  HelperText,
  HelperTextItem,
  FormHelperText,
  Button,
  FormSelect,
  FormSelectOption,
} from '@patternfly/react-core';
import { ExclamationCircleIcon } from '@patternfly/react-icons';
import { useState, useRef } from 'react';
import { formatDateTime } from '@/utils/dateTime';
import {
  OffsetValue,
  TopicSelection,
  PartitionSelection,
} from './types';

interface OffsetValueSelectorProps {
  offsetValue?: OffsetValue;
  customOffset?: number;
  dateTime?: string;
  dateTimeDisplayMode?: 'utc' | 'local';
  topicSelection: TopicSelection;
  partitionSelection: PartitionSelection;
  onOffsetValueChange: (value?: OffsetValue) => void;
  onCustomOffsetChange: (value: number) => void;
  onDateTimeChange: (value: string) => void;
  onDateTimeDisplayModeChange: (mode: 'utc' | 'local') => void;
  errors?: {
    customOffset?: string;
    dateTime?: string;
  };
}

export function OffsetValueSelector({
  offsetValue,
  customOffset,
  dateTime,
  dateTimeDisplayMode = 'utc',
  topicSelection,
  partitionSelection,
  onOffsetValueChange,
  onCustomOffsetChange,
  onDateTimeChange,
  onDateTimeDisplayModeChange,
  errors,
}: OffsetValueSelectorProps) {
  const { t } = useTranslation();
  const [isOffsetSelectOpen, setIsOffsetSelectOpen] = useState(false);
  const offsetToggleRef = useRef<HTMLButtonElement | null>(null);
  const offsetMenuRef = useRef<HTMLDivElement | null>(null);

  // Determine available offset options based on topic/partition selection
  const offsetOptions: Array<{ value: OffsetValue; label: string }> = [];

  // Custom offset only available when specific topic and partition selected
  if (
    topicSelection === 'selectedTopic' &&
    partitionSelection === 'selectedPartition'
  ) {
    offsetOptions.push({
      value: 'custom',
      label: t('groups.resetOffset.offset.custom'),
    });
  }

  // Always available options
  offsetOptions.push(
    { value: 'earliest', label: t('groups.resetOffset.offset.earliest') },
    { value: 'latest', label: t('groups.resetOffset.offset.latest') },
    {
      value: 'dateTimeIso',
      label: t('groups.resetOffset.offset.dateTimeIso'),
    },
    {
      value: 'dateTimeEpoch',
      label: t('groups.resetOffset.offset.dateTimeEpoch'),
    }
  );

  // Delete only available when specific topic selected
  if (topicSelection === 'selectedTopic') {
    offsetOptions.push({
      value: 'delete',
      label: t('groups.resetOffset.offset.delete'),
    });
  }

  const selectedOffsetLabel =
    offsetOptions.find((opt) => opt.value === offsetValue)?.label ||
    t('groups.resetOffset.selectOffset');

  const offsetMenuItems = offsetOptions.map((option) => (
    <SelectOption
      key={option.value}
      itemId={option.value}
      isSelected={offsetValue === option.value}
    >
      {option.label}
    </SelectOption>
  ));

  const offsetToggle = (
    <MenuToggle
      ref={offsetToggleRef}
      onClick={() => setIsOffsetSelectOpen(!isOffsetSelectOpen)}
      isExpanded={isOffsetSelectOpen}
      isFullWidth
      isPlaceholder={!offsetValue}
    >
      {selectedOffsetLabel}
    </MenuToggle>
  );

  const offsetMenu = (
    <Menu
      ref={offsetMenuRef}
      onSelect={(_event, itemId) => {
        onOffsetValueChange(itemId as OffsetValue);
        setIsOffsetSelectOpen(false);
      }}
      activeItemId={offsetValue}
    >
      <MenuContent>
        <MenuList>{offsetMenuItems}</MenuList>
      </MenuContent>
    </Menu>
  );

  const formatLocalDateTime = (value: Date | string) =>
    formatDateTime({
      value,
      format: "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    });

  const handleUseCurrentDateTime = () => {
    if (offsetValue === 'dateTimeEpoch') {
      onDateTimeChange(Date.now().toString());
      return;
    }

    if (offsetValue === 'dateTimeIso') {
      const now = new Date();
      onDateTimeChange(
        dateTimeDisplayMode === 'local'
          ? formatLocalDateTime(now)
          : now.toISOString()
      );
    }
  };

  const handleDateTimeDisplayModeChange = (mode: 'utc' | 'local') => {
    if (dateTime) {
      const parsedDate = new Date(dateTime);

      if (!isNaN(parsedDate.getTime())) {
        onDateTimeChange(
          mode === 'local'
            ? formatLocalDateTime(parsedDate)
            : parsedDate.toISOString()
        );
      }
    }

    onDateTimeDisplayModeChange(mode);
  };

  return (
    <>
      <FormGroup
        label={t('groups.resetOffset.newOffset')}
        isRequired
        fieldId="offset-select"
      >
        <MenuContainer
          toggle={offsetToggle}
          toggleRef={offsetToggleRef}
          menu={offsetMenu}
          menuRef={offsetMenuRef}
          isOpen={isOffsetSelectOpen}
          onOpenChange={setIsOffsetSelectOpen}
          onOpenChangeKeys={['Escape']}
        />
      </FormGroup>

      {offsetValue === 'custom' && (
        <FormGroup
          label={t('groups.resetOffset.customOffset')}
          isRequired
          fieldId="custom-offset-input"
        >
          <TextInput
            id="custom-offset-input"
            type="number"
            value={customOffset?.toString() || ''}
            onChange={(_event, value) => {
              const numValue = parseInt(value, 10);
              if (!isNaN(numValue) && numValue >= 0) {
                onCustomOffsetChange(numValue);
              } else if (value === '') {
                onCustomOffsetChange(0);
              }
            }}
            placeholder={t('groups.resetOffset.customOffsetPlaceholder')}
            validated={errors?.customOffset ? 'error' : 'default'}
          />
          {!errors?.customOffset && (
            <FormHelperText>
              <HelperText>
                <HelperTextItem>
                  {t('groups.resetOffset.customOffsetHelper')}
                </HelperTextItem>
              </HelperText>
            </FormHelperText>
          )}
          {errors?.customOffset && (
            <FormHelperText>
              <HelperText>
                <HelperTextItem icon={<ExclamationCircleIcon />} variant="error">
                  {errors.customOffset}
                </HelperTextItem>
              </HelperText>
            </FormHelperText>
          )}
        </FormGroup>
      )}

      {(offsetValue === 'dateTimeIso' || offsetValue === 'dateTimeEpoch') && (
        <FormGroup
          label={t('groups.resetOffset.dateTime')}
          isRequired
          fieldId="datetime-input"
        >
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-start' }}>
            {offsetValue === 'dateTimeIso' && (
              <div style={{ width: '7rem', flex: '0 0 7rem' }}>
                <FormSelect
                  value={dateTimeDisplayMode}
                  onChange={(_event, value) =>
                    handleDateTimeDisplayModeChange(value as 'utc' | 'local')
                  }
                  aria-label={t('groups.resetOffset.timeZoneMode')}
                >
                  <FormSelectOption
                    value="utc"
                    label={t('groups.resetOffset.timeZoneUtc')}
                  />
                  <FormSelectOption
                    value="local"
                    label={t('groups.resetOffset.timeZoneLocal')}
                  />
                </FormSelect>
              </div>
            )}
            <div style={{ flex: 1 }}>
              <TextInput
                id="datetime-input"
                type={offsetValue === 'dateTimeEpoch' ? 'number' : 'text'}
                value={dateTime || ''}
                onChange={(_event, value) => onDateTimeChange(value)}
                placeholder={
                  offsetValue === 'dateTimeIso'
                    ? t('groups.resetOffset.dateTimePlaceholder')
                    : t('groups.resetOffset.epochPlaceholder')
                }
                validated={errors?.dateTime ? 'error' : 'default'}
              />
            </div>
            <Button variant="secondary" onClick={handleUseCurrentDateTime}>
              {t('groups.resetOffset.useCurrentDateTime')}
            </Button>
          </div>
          {!errors?.dateTime && (
            <FormHelperText>
              <HelperText>
                {offsetValue === 'dateTimeIso' ? (
                  <HelperTextItem>
                    {dateTimeDisplayMode === 'local'
                      ? t('groups.resetOffset.dateTimeHelperLocal')
                      : t('groups.resetOffset.dateTimeHelperUtc')}
                  </HelperTextItem>
                ) : (
                  <HelperTextItem>
                    {t('groups.resetOffset.epochHelper')}
                  </HelperTextItem>
                )}
              </HelperText>
            </FormHelperText>
          )}
          {errors?.dateTime && (
            <FormHelperText>
              <HelperText>
                <HelperTextItem icon={<ExclamationCircleIcon />} variant="error">
                  {errors.dateTime}
                </HelperTextItem>
              </HelperText>
            </FormHelperText>
          )}
        </FormGroup>
      )}
    </>
  );
}