/**
 * Fetch-from selector.
 *
 * Controlled component: the selected fetch option comes in as a prop,
 * and changes go out through onFromChange.
 */

import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Menu,
  MenuContainer,
  MenuContent,
  MenuList,
  MenuToggle,
  SelectOption,
} from '@patternfly/react-core';

export type FromOptions =
  | 'latest'
  | 'last5'
  | 'last10'
  | 'last15'
  | 'offset'
  | 'epoch'
  | 'timestamp';

interface FetchFromSelectorProps {
  selectedFrom?: FromOptions;
  onFromChange: (from: FromOptions) => void;
  isDisabled?: boolean;
  ouiaId?: string;
}

export function FetchFromSelector({
  selectedFrom = 'latest',
  onFromChange,
  isDisabled = false,
  ouiaId = 'fetch-from-select',
}: FetchFromSelectorProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);

  const FROM_OPTIONS: { value: FromOptions; label: string }[] = [
    { value: 'latest', label: t('topics.messages.fetchFrom.latest') },
    { value: 'last5', label: t('topics.messages.fetchFrom.last5') },
    { value: 'last10', label: t('topics.messages.fetchFrom.last10') },
    { value: 'last15', label: t('topics.messages.fetchFrom.last15') },
    { value: 'offset', label: t('topics.messages.fetchFrom.offset') },
    {
      value: 'epoch',
      label: t('topics.messages.fetchFrom.epoch'),
    },
    {
      value: 'timestamp',
      label: t('topics.messages.fetchFrom.timestamp'),
    },
  ];

  const toggleRef = useRef<HTMLButtonElement | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);

  const selectedOption = FROM_OPTIONS.find(
    (option) => option.value === selectedFrom
  );

  const toggle = (
    <MenuToggle
      ref={toggleRef}
      ouiaId={ouiaId}
      onClick={() => setIsOpen(!isOpen)}
      isExpanded={isOpen}
      isDisabled={isDisabled}
      aria-label={t('topics.messages.fetchFrom.label')}
    >
      {selectedOption?.label ?? FROM_OPTIONS[0].label}
    </MenuToggle>
  );

  const menu = (
    <Menu
      ref={menuRef}
      onSelect={(_event, itemId) => {
        onFromChange(itemId as FromOptions);
        setIsOpen(false);
      }}
      activeItemId={selectedFrom}
    >
      <MenuContent>
        <MenuList>
          {FROM_OPTIONS.map((option) => (
            <SelectOption
              key={option.value}
              itemId={option.value}
              isSelected={selectedFrom === option.value}
            >
              {option.label}
            </SelectOption>
          ))}
        </MenuList>
      </MenuContent>
    </Menu>
  );

  return (
    <MenuContainer
      toggle={toggle}
      toggleRef={toggleRef}
      menu={menu}
      menuRef={menuRef}
      isOpen={isOpen}
      onOpenChange={setIsOpen}
      onOpenChangeKeys={['Escape']}
    />
  );
}
