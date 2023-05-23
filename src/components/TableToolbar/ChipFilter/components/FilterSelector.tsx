import type { SelectProps } from "@patternfly/react-core";
import { Select, SelectOption } from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";
import type { VoidFunctionComponent } from "react";
import { useState } from "react";

export const FilterSelector: VoidFunctionComponent<{
  options: string[];
  value: string;
  onChange: (value: string) => void;
  ouiaId: string;
}> = ({ options, value, onChange, ouiaId }) => {
  const [isOpen, setIsOpen] = useState(false);

  const onSelect: SelectProps["onSelect"] = (_, value) => {
    onChange(value as string);
    setIsOpen(false);
  };

  return (
    <Select
      toggleIcon={<FilterIcon />}
      aria-label={"table:select_filter"}
      selections={value}
      isOpen={isOpen}
      onSelect={onSelect}
      onToggle={setIsOpen}
      ouiaId={ouiaId}
    >
      {options.map((option, index) => (
        <SelectOption value={option} key={index}>
          {option}
        </SelectOption>
      ))}
    </Select>
  );
};
