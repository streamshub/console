/**
 * Filter By Broker Component
 * 
 * Dropdown selector for filtering charts by specific broker/node.
 * Allows users to view metrics for individual nodes or all nodes.
 */

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Select,
  SelectOption,
  SelectList,
  MenuToggle,
  MenuToggleElement,
} from '@patternfly/react-core';

interface FilterByBrokerProps {
  brokerIds: string[];
  value: string | null; // null means "All Brokers"
  onChange: (brokerId: string | null) => void;
  isDisabled?: boolean;
}

export function FilterByBroker({
  brokerIds,
  value,
  onChange,
  isDisabled = false,
}: FilterByBrokerProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);

  const selectedLabel = value
    ? `Node ${value}`
    : t('metrics.all_brokers') || 'All brokers';

  const onToggleClick = () => {
    setIsOpen(!isOpen);
  };

  const onSelect = (_event: React.MouseEvent<Element, MouseEvent> | undefined, selectedValue: string | number | undefined) => {
    if (selectedValue === 'all') {
      onChange(null);
    } else if (typeof selectedValue === 'string') {
      onChange(selectedValue);
    }
    setIsOpen(false);
  };

  return (
    <Select
      id="broker-select"
      isOpen={isOpen}
      selected={value || 'all'}
      onSelect={onSelect}
      onOpenChange={(isOpen) => setIsOpen(isOpen)}
      toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
        <MenuToggle
          ref={toggleRef}
          onClick={onToggleClick}
          isExpanded={isOpen}
          isDisabled={isDisabled}
        >
          {selectedLabel}
        </MenuToggle>
      )}
      shouldFocusToggleOnSelect
    >
      <SelectList>
        <SelectOption key="all" value="all">
          {t('metrics.all_brokers') || 'All brokers'}
        </SelectOption>
        {brokerIds.map((brokerId) => (
          <SelectOption key={brokerId} value={brokerId}>
            Node {brokerId}
          </SelectOption>
        ))}
      </SelectList>
    </Select>
  );
}