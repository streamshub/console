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
  Radio,
  HelperText,
  HelperTextItem,
  FormHelperText,
} from '@patternfly/react-core';
import { ExclamationCircleIcon } from '@patternfly/react-icons';
import { useState, useRef } from 'react';
import { OffsetValue, DateTimeFormat, TopicSelection, PartitionSelection } from '../../api/types';

interface OffsetValueSelectorProps {
  offsetValue: OffsetValue;
  customOffset?: number;
  dateTime?: string;
  dateTimeFormat: DateTimeFormat;
  topicSelection: TopicSelection;
  partitionSelection: PartitionSelection;
  onOffsetValueChange: (value: OffsetValue) => void;
  onCustomOffsetChange: (value: number) => void;
  onDateTimeChange: (value: string) => void;
  onDateTimeFormatChange: (format: DateTimeFormat) => void;
  errors?: {
    customOffset?: string;
    dateTime?: string;
  };
}

export function OffsetValueSelector({
  offsetValue,
  customOffset,
  dateTime,
  dateTimeFormat,
  topicSelection,
  partitionSelection,
  onOffsetValueChange,
  onCustomOffsetChange,
  onDateTimeChange,
  onDateTimeFormatChange,
  errors,
}: OffsetValueSelectorProps) {
  const { t } = useTranslation();
  const [isOffsetSelectOpen, setIsOffsetSelectOpen] = useState(false);
  const offsetToggleRef = useRef<any>();
  const offsetMenuRef = useRef<any>();

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
      value: 'specificDateTime',
      label: t('groups.resetOffset.offset.specificDateTime'),
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

      {offsetValue === 'specificDateTime' && (
        <>
          <FormGroup
            label={t('groups.resetOffset.selectDateTime')}
            isInline
            role="radiogroup"
          >
            <Radio
              id="iso-format-radio"
              name="datetime-format"
              label={t('groups.resetOffset.isoDateFormat')}
              isChecked={dateTimeFormat === 'ISO'}
              onChange={() => onDateTimeFormatChange('ISO')}
            />
            <Radio
              id="epoch-format-radio"
              name="datetime-format"
              label={t('groups.resetOffset.unixDateFormat')}
              isChecked={dateTimeFormat === 'Epoch'}
              onChange={() => onDateTimeFormatChange('Epoch')}
            />
          </FormGroup>

          <FormGroup
            label={
              dateTimeFormat === 'ISO'
                ? t('groups.resetOffset.isoDateFormat')
                : t('groups.resetOffset.unixDateFormat')
            }
            isRequired
            fieldId="datetime-input"
          >
            <TextInput
              id="datetime-input"
              type={dateTimeFormat === 'Epoch' ? 'number' : 'text'}
              value={dateTime || ''}
              onChange={(_event, value) => onDateTimeChange(value)}
              placeholder={
                dateTimeFormat === 'ISO'
                  ? t('groups.resetOffset.dateTimePlaceholder')
                  : t('groups.resetOffset.epochPlaceholder')
              }
              validated={errors?.dateTime ? 'error' : 'default'}
            />
            {!errors?.dateTime && (
              <FormHelperText>
                <HelperText>
                  <HelperTextItem>
                    {dateTimeFormat === 'ISO'
                      ? t('groups.resetOffset.dateTimeHelper')
                      : t('groups.resetOffset.epochHelper')}
                  </HelperTextItem>
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
        </>
      )}
    </>
  );
}