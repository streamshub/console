import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  MenuToggle,
  MenuToggleElement,
  Select,
  SelectList,
  SelectOption,
} from '@patternfly/react-core';

export type WhereOption = 'key' | 'headers' | 'value' | undefined;

interface WhereSelectorProps {
  where: WhereOption;
  onWhereChange: (where: WhereOption) => void;
  ouiaId?: string;
  isDisabled?: boolean;
}

type WhereValue = 'everywhere' | 'key' | 'headers' | 'value';

export function WhereSelector({
  where,
  onWhereChange,
  ouiaId = 'where-selector',
  isDisabled,
}: WhereSelectorProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);

  const options: { value: WhereValue; label: string }[] = [
    {
      value: 'everywhere',
      label: t('topics.messages.advancedSearch.whereEverywhere'),
    },
    {
      value: 'key',
      label: t('topics.messages.advancedSearch.whereKey'),
    },
    {
      value: 'headers',
      label: t('topics.messages.advancedSearch.whereHeaders'),
    },
    {
      value: 'value',
      label: t('topics.messages.advancedSearch.whereValue'),
    },
  ];

  const selected = where ?? 'everywhere';
  const selectedLabel =
    options.find((option) => option.value === selected)?.label ??
    options[0].label;

  return (
    <Select
      isOpen={isOpen}
      onOpenChange={setIsOpen}
      selected={selected}
      onSelect={(_, selection) => {
        const value = selection as WhereValue;
        onWhereChange(value === 'everywhere' ? undefined : value);
        setIsOpen(false);
      }}
      toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
        <MenuToggle
          ref={toggleRef}
          ouiaId={ouiaId}
          isDisabled={isDisabled}
          onClick={() => setIsOpen((open) => !open)}
          isExpanded={isOpen}
          style={{ width: '140px' }}
        >
          {selectedLabel}
        </MenuToggle>
      )}
    >
      <SelectList>
        {options.map(({ value, label }) => (
          <SelectOption key={value} value={value}>
            {label}
          </SelectOption>
        ))}
      </SelectList>
    </Select>
  );
}
