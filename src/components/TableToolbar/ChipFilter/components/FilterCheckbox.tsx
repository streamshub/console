import { Select, SelectOption, SelectVariant } from "@patternfly/react-core";
import type { FunctionComponent } from "react";
import { useState } from "react";
import type { CheckboxType } from "../types";

export const FilterCheckbox: FunctionComponent<
  Pick<CheckboxType<any>, "chips" | "options" | "onToggle"> & {
    label: string;
  }
> = ({ label, chips, options, onToggle }) => {
  const [isOpen, setIsOpen] = useState(false);
  return (
    <Select
      variant={SelectVariant.checkbox}
      aria-label={label}
      toggleAriaLabel={`Search for ${label}`}
      onToggle={setIsOpen}
      onSelect={(_, value) => {
        onToggle(value);
      }}
      selections={chips}
      isOpen={isOpen}
      placeholderText={`Search for ${label}`}
      isCheckboxSelectionBadgeHidden={true}
    >
      {Object.entries(options).map(([key, label]) => (
        <SelectOption key={key} value={key}>
          {label}
        </SelectOption>
      ))}
    </Select>
  );
};
