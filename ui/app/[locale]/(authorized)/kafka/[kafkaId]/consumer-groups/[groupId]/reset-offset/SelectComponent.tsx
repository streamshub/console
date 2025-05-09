import React, { useState } from "react";
import {
  Select,
  SelectOption,
  SelectList,
  MenuToggle,
  MenuToggleElement,
  SelectProps,
} from "@/libs/patternfly/react-core";

export type SelectComponentProps<T extends string | number> = {
  options: { value: T; label: string }[];
  value: T;
  onChange: (value: T) => void;
  placeholder?: string;
};

export function SelectComponent<T extends string | number>({
  options,
  value,
  onChange,
  placeholder = "Select a value",
}: SelectComponentProps<T>) {
  const [isOpen, setIsOpen] = useState(false);

  const onToggle = () => setIsOpen((prev) => !prev);

  const onSelect: SelectProps["onSelect"] = (_, selection) => {
    onChange(selection as T);
    setIsOpen(false);
  };

  const toggle = (toggleRef: React.Ref<MenuToggleElement>) => (
    <MenuToggle ref={toggleRef} onClick={onToggle} isExpanded={isOpen}>
      {options.find((option) => option.value === value)?.label || placeholder}
    </MenuToggle>
  );

  return (
    <Select
      isOpen={isOpen}
      selected={value}
      onSelect={onSelect}
      toggle={toggle}
      onOpenChange={(isOpen) => setIsOpen(isOpen)}
      shouldFocusToggleOnSelect
    >
      <SelectList>
        {options.map((option) => (
          <SelectOption key={String(option.value)} value={option.value}>
            {option.label}
          </SelectOption>
        ))}
      </SelectList>
    </Select>
  );
}
