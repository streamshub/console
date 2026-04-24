/**
 * Filter By Time Component
 * 
 * Dropdown selector for time range filtering on charts.
 * Allows users to select different time windows for metrics display.
 */

import { useState } from 'react';
import {
  Select,
  SelectOption,
  SelectList,
  MenuToggle,
  MenuToggleElement,
} from '@patternfly/react-core';
import { DurationOptions } from '@/components/kafka/overview/utils/types';

interface FilterByTimeProps {
  value: DurationOptions;
  onChange: (duration: DurationOptions) => void;
  isDisabled?: boolean;
}

const timeOptions = [
  { value: DurationOptions.Last5minutes, label: 'Last 5 minutes' },
  { value: DurationOptions.Last15minutes, label: 'Last 15 minutes' },
  { value: DurationOptions.Last30minutes, label: 'Last 30 minutes' },
  { value: DurationOptions.Last1hour, label: 'Last 1 hour' },
  { value: DurationOptions.Last3hours, label: 'Last 3 hours' },
  { value: DurationOptions.Last6hours, label: 'Last 6 hours' },
  { value: DurationOptions.Last12hours, label: 'Last 12 hours' },
  { value: DurationOptions.Last24hours, label: 'Last 24 hours' },
  { value: DurationOptions.Last2days, label: 'Last 2 days' },
  { value: DurationOptions.Last7days, label: 'Last 7 days' },
];

export function FilterByTime({
  value,
  onChange,
  isDisabled = false,
}: FilterByTimeProps) {
  const [isOpen, setIsOpen] = useState(false);

  const selectedLabel =
    timeOptions.find((opt) => opt.value === value)?.label || 'Select time range';

  const onToggleClick = () => {
    setIsOpen(!isOpen);
  };

  const onSelect = (_event: React.MouseEvent<Element, MouseEvent> | undefined, value: string | number | undefined) => {
    if (typeof value === 'number') {
      onChange(value as DurationOptions);
      setIsOpen(false);
    }
  };

  return (
    <Select
      id="time-range-select"
      isOpen={isOpen}
      selected={value}
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
        {timeOptions.map((option) => (
          <SelectOption key={option.value} value={option.value}>
            {option.label}
          </SelectOption>
        ))}
      </SelectList>
    </Select>
  );
}