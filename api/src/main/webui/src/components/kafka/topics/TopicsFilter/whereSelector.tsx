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
}

export function WhereSelector({
  where,
  onWhereChange,
  ouiaId = 'where-selector',
}: WhereSelectorProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);

  const labels: Record<'everywhere' | 'key' | 'headers' | 'value', string> = {
    everywhere: 'Anywhere',
    key: t('topics.messages.advancedSearch.whereKey'),
    headers: t('topics.messages.advancedSearch.whereHeaders'),
    value: t('topics.messages.advancedSearch.whereValue'),
  };

  return (
    <Select
      isOpen={isOpen}
      onOpenChange={setIsOpen}
      selected={where ?? 'everywhere'}
      onSelect={(_, value) => {
        onWhereChange(
          value === 'everywhere' ? undefined : (value as WhereOption)
        );
        setIsOpen(false);
      }}
      toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
        <MenuToggle
          ref={toggleRef}
          ouiaId={ouiaId}
          onClick={() => setIsOpen(!isOpen)}
          isExpanded={isOpen}
          style={{ width: '140px' }}
        >
          {labels[where ?? 'everywhere']}
        </MenuToggle>
      )}
    >
      <SelectList>
        {(Object.keys(labels) as Array<keyof typeof labels>).map((key) => (
          <SelectOption key={key} value={key}>
            {labels[key]}
          </SelectOption>
        ))}
      </SelectList>
    </Select>
  );
}
