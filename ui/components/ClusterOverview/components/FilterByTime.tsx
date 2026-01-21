import {
  MenuToggle,
  MenuToggleElement,
  Select,
  SelectList,
  SelectOption,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { useState } from "react";

export enum DurationOptions {
  Last5minutes = 5,
  Last15minutes = 15,
  Last30minutes = 30,
  Last1hour = 60,
  Last3hours = 3 * 60,
  Last6hours = 6 * 60,
  Last12hours = 12 * 60,
  Last24hours = 24 * 60,
  Last2days = 2 * 24 * 60,
  Last7days = 7 * 24 * 60,
}

export const DurationOptionsMap = {
  [DurationOptions.Last5minutes]: "Last 5 minutes",
  [DurationOptions.Last15minutes]: "Last 15 minutes",
  [DurationOptions.Last30minutes]: "Last 30 minutes",
  [DurationOptions.Last1hour]: "Last 1 hour",
  [DurationOptions.Last3hours]: "Last 3 hours",
  [DurationOptions.Last6hours]: "Last 6 hours",
  [DurationOptions.Last12hours]: "Last 12 hours",
  [DurationOptions.Last24hours]: "Last 24 hours",
  [DurationOptions.Last2days]: "Last 2 days",
  [DurationOptions.Last7days]: "Last 7 days",
} as const;

export function FilterByTime({
  duration,
  keyText,
  ariaLabel,
  disableToolbar,
  onDurationChange,
}: {
  duration: DurationOptions;
  onDurationChange: (value: DurationOptions) => void;
  keyText: string;
  ariaLabel: string;
  disableToolbar: boolean;
}) {
  const [isTimeSelectOpen, setIsTimeSelectOpen] = useState(false);

  const onToggleClick = () => setIsTimeSelectOpen((prev) => !prev);

  const onTimeSelect = (
    _event: React.MouseEvent<Element, MouseEvent> | undefined,
    value: string | number | undefined,
  ) => {
    const mapping = Object.entries(DurationOptionsMap).find(
      ([, label]) => label === value,
    );
    if (mapping) {
      onDurationChange(parseInt(mapping[0], 10) as DurationOptions);
    }
    setIsTimeSelectOpen(false);
  };

  const toggle = (toggleRef: React.Ref<MenuToggleElement>) => (
    <MenuToggle
      ref={toggleRef}
      onClick={onToggleClick}
      isExpanded={isTimeSelectOpen}
      isDisabled={disableToolbar}
    >
      {DurationOptionsMap[duration]}
    </MenuToggle>
  );

  return (
    <ToolbarItem>
      <label hidden id={`${keyText}-label`}>
        {ariaLabel}
      </label>
      <Select
        id={`filter-by-time-${keyText}`}
        isOpen={isTimeSelectOpen}
        selected={DurationOptionsMap[duration]}
        onSelect={onTimeSelect}
        onOpenChange={setIsTimeSelectOpen}
        toggle={toggle}
        shouldFocusToggleOnSelect
      >
        <SelectList>
          {Object.values(DurationOptionsMap).map((label, idx) => (
            <SelectOption key={`${keyText}-${idx}`} value={label}>
              {label}
            </SelectOption>
          ))}
        </SelectList>
      </Select>
    </ToolbarItem>
  );
}
