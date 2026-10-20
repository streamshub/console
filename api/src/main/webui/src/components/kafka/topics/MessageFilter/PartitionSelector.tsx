import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Divider,
  Menu,
  MenuContainer,
  MenuContent,
  MenuList,
  MenuToggle,
  SelectOption,
} from '@patternfly/react-core';

interface PartitionSelectorProps {
  partitions: number[];
  selectedPartition?: number;
  onPartitionChange: (partition?: number) => void;
  isDisabled?: boolean;
  ouiaId?: string;
}

export function PartitionSelector({
  partitions,
  selectedPartition,
  onPartitionChange,
  isDisabled = false,
  ouiaId = 'partition-select',
}: PartitionSelectorProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);

  const toggleRef = useRef<HTMLButtonElement | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);

  const toggle = (
    <MenuToggle
      ref={toggleRef}
      ouiaId={ouiaId}
      onClick={() => setIsOpen(!isOpen)}
      isExpanded={isOpen}
      isDisabled={isDisabled}
      isFullWidth
      aria-label={t('topics.messages.partitionPlaceholder')}
    >
      {selectedPartition !== undefined
        ? t('topics.messages.partitionOption', { value: selectedPartition })
        : t('topics.messages.partitionPlaceholder')}
    </MenuToggle>
  );

  const menu = (
    <Menu
      ref={menuRef}
      onSelect={(_event, itemId) => {
        onPartitionChange(itemId === '' ? undefined : Number(itemId));
        setIsOpen(false);
      }}
      activeItemId={selectedPartition ?? ''}
      isScrollable
    >
      <MenuContent maxMenuHeight="200px">
        <MenuList>
          <SelectOption itemId="" isSelected={selectedPartition === undefined}>
            {t('topics.messages.partitionPlaceholder')}
          </SelectOption>
          <Divider />
          {partitions.map((partition) => (
            <SelectOption
              key={partition}
              itemId={partition}
              isSelected={selectedPartition === partition}
            >
              {partition}
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
