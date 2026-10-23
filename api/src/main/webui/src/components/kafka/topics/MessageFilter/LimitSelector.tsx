import { useState } from 'react';
import {
  MenuToggle,
  MenuToggleElement,
  Select,
  SelectList,
  SelectOption,
} from '@patternfly/react-core';

// 50, 100, 150 ... 1000
const LIMIT_OPTIONS = Array.from({ length: 20 }, (_, i) => (i + 1) * 50);

interface LimitSelectProps {
  value: number;
  onChange: (limit: number) => void;
  ouiaId?: string;
}

export function LimitSelect({
  value,
  onChange,
  ouiaId = 'limit-select',
}: LimitSelectProps) {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <Select
      isOpen={isOpen}
      onOpenChange={setIsOpen}
      isScrollable
      maxMenuHeight="200px"
      selected={value}
      onSelect={(_, selection) => {
        onChange(selection as number);
        setIsOpen(false);
      }}
      toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
        <MenuToggle
          ref={toggleRef}
          ouiaId={ouiaId}
          onClick={() => setIsOpen(!isOpen)}
          isExpanded={isOpen}
          style={{ width: '120px' }}
        >
          {value}
        </MenuToggle>
      )}
    >
      <SelectList>
        {LIMIT_OPTIONS.map((option) => (
          <SelectOption key={option} value={option}>
            {option}
          </SelectOption>
        ))}
      </SelectList>
    </Select>
  );
}
